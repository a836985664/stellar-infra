package com.example.user.drools;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

/**
 * Drools 规则引擎 Demo。
 *
 * 业务场景：电商订单价格计算。
 * 规则在 DRL 字符串中定义，KieHelper 直接编译并生成 KieSession 执行。
 *
 * 覆盖的 Drools 核心概念：
 *   1. 普通规则匹配（when 条件 + then 动作）
 *   2. salience（规则优先级）
 *   3. no-loop（防止无限循环）
 *   4. modify（修改 Fact 后重新触发规则）
 *   5. global（全局变量跨规则共享）
 */
public class DroolsDemo {

    public static void main(String[] args) {
        // 1. 定义规则（DRL 字符串）
        String drl =
                "package com.example.user.drools;\n" +
                "import com.example.user.drools.Order;\n" +
                "\n" +
                "// 规则1：白银会员 95 折（优先级最高，先执行）\n" +
                "rule \"白银会员折扣\"\n" +
                "    salience 30\n" +
                "    when\n" +
                "        $o : Order( vipLevel == 1, finalPrice == 0.0 )\n" +
                "    then\n" +
                "        $o.setDiscountTag(\"白银95折\");\n" +
                "        modify($o) { setFinalPrice($o.getAmount() * 0.95) };\n" +
                "end\n" +
                "\n" +
                "// 规则2：黄金及以上会员 85 折\n" +
                "rule \"黄金会员折扣\"\n" +
                "    salience 20\n" +
                "    when\n" +
                "        $o : Order( vipLevel >= 2, finalPrice == 0.0 )\n" +
                "    then\n" +
                "        $o.setDiscountTag(\"黄金85折\");\n" +
                "        modify($o) { setFinalPrice($o.getAmount() * 0.85) };\n" +
                "end\n" +
                "\n" +
                "// 规则3：普通会员大额订单满 500 减 50\n" +
                "rule \"满500减50\"\n" +
                "    salience 10\n" +
                "    when\n" +
                "        $o : Order( vipLevel == 0, amount >= 500, finalPrice == 0.0 )\n" +
                "    then\n" +
                "        $o.setDiscountTag(\"满500减50\");\n" +
                "        modify($o) { setFinalPrice($o.getAmount() - 50) };\n" +
                "end\n" +
                "\n" +
                "// 规则4：使用优惠券再减 10 元（在已有折扣基础上）\n" +
                "rule \"优惠券减10元\"\n" +
                "    salience 5\n" +
                "    when\n" +
                "        $o : Order( coupon == true, finalPrice > 0.0 )\n" +
                "    then\n" +
                "        System.out.println(\"  [规则] \" + $o.getDiscountTag() + \" 基础上，优惠券再减10元\");\n" +
                "        modify($o) { setFinalPrice($o.getFinalPrice() - 10) };\n" +
                "end\n" +
                "\n" +
                "// 规则5：兜底——没命中任何折扣的订单按原价\n" +
                "rule \"无折扣兜底\"\n" +
                "    salience -10\n" +
                "    no-loop true\n" +
                "    when\n" +
                "        $o : Order( finalPrice == 0.0 )\n" +
                "    then\n" +
                "        $o.setDiscountTag(\"无折扣\");\n" +
                "        $o.setFinalPrice($o.getAmount());\n" +
                "        System.out.println(\"  [规则] 未命中任何促销，按原价\");\n" +
                "end";

        // 2. 编译规则并校验
        KieHelper helper = new KieHelper();
        helper.addContent(drl, ResourceType.DRL);
        Results results = helper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            results.getMessages().forEach(m -> System.err.println("[编译错误] " + m.getText()));
            return;
        }

        // 3. 构建 KieBase，创建会话
        KieBase kieBase = helper.build();
        KieSession kieSession = kieBase.newKieSession();

        // 4. 插入多个 Fact
        Order o1 = new Order(200.0, 0, false, 0.0, null);    // 普通会员，小额，无券 → 无折扣
        Order o2 = new Order(600.0, 0, false, 0.0, null);    // 普通会员，满500 → 减50
        Order o3 = new Order(1000.0, 1, true, 0.0, null);    // 白银会员，用券 → 95折再减10
        Order o4 = new Order(3000.0, 3, true, 0.0, null);    // 钻石会员，用券 → 85折再减10

        kieSession.insert(o1);
        kieSession.insert(o2);
        kieSession.insert(o3);
        kieSession.insert(o4);

        // 5. 触发所有规则
        System.out.println("===== 订单结算 =====");
        int fired = kieSession.fireAllRules();
        System.out.println("共触发规则: " + fired + " 次");

        // 6. 输出结果
        print(o1);
        print(o2);
        print(o3);
        print(o4);

        kieSession.dispose();
    }

    private static void print(Order o) {
        System.out.printf("  原始金额=%.2f, VIP=%d, 用券=%b -> 应付=%.2f (%s)%n",
                o.getAmount(), o.getVipLevel(), o.isCoupon(), o.getFinalPrice(), o.getDiscountTag());
    }
}