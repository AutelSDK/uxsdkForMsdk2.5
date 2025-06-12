package com.autel.setting.enums

/**
 * @author 
 * @date 2023/7/21
 * 国家码
 */
enum class CountryCodeEnum(val code: String) {
    Other("OTHER"),

    /**
     * 中国
     */
    China("CN"),

    /**
     * 台湾
     */
    TaiWan("TW"),

    /**
     * 美国
     */
    America("US"),

    /**
     * 加拿大
     */
    Canada("CA"),

    /**
     * 英国
     */
    UnitedKingdom("GB"),

    /**
     * 澳大利亚
     */
    Australia("AU"),

    /**
     * 韩国
     */
    Korea("KR"),

    /**
     * 日本
     */
    Japan("JP"),

    /**
     * 俄罗斯
     */
    Russia("RU"),

    /**
     * 奥地利
     */
    EU_Austria("AT"),

    /**
     * 比利时
     */
    EU_Belgium("BE"),

    /**
     * 保加利亚
     */
    EU_Bulgaria("BG"),

    /**
     * 塞浦路斯
     */
    EU_Cyprus("CY"),

    /**
     * 克罗地亚
     */
    EU_Croatia("HR"),

    /**
     * 捷克
     */
    EU_Czechia("CZ"),

    /**
     * 丹麦
     */
    EU_Denmark("DK"),

    /**
     * 爱沙尼亚
     */
    EU_Estonia("EE"),

    /**
     * 芬兰
     */
    EU_Finland("FI"),

    /**
     * 法国
     */
    EU_France("FR"),

    /**
     * 德国
     */
    EU_Germany("DE"),

    /**
     * 希腊
     */
    EU_Greece("GR"),

    /**
     * 匈牙利
     */
    EU_Hungary("HU"),

    /**
     * 爱尔兰
     */
    EU_Ireland("IE"),

    /**
     * 意大利
     */
    EU_Italy("IT"),

    /**
     * 拉脱维亚
     */
    EU_Latvia("LV"),

    /**
     * 立陶宛
     */
    EU_Lithuania("LT"),

    /**
     * 卢森堡
     */
    EU_Luxembourg("LU"),

    /**
     * 马耳他
     */
    EU_Malta("MT"),

    /**
     * 波兰
     */
    EU_Poland("PL"),

    /**
     * 葡萄牙
     */
    EU_Portugal("PT"),

    /**
     * 罗马尼亚
     */
    EU_Romania("RO"),

    /**
     * 斯洛伐克
     */
    EU_Slovakia("SK"),

    /**
     * 斯洛文尼亚
     */
    EU_Slovenia("SI"),

    /**
     * 西班牙
     */
    EU_Spain("ES"),

    /**
     * 瑞典
     */
    EU_Sweden("SE"),

    /**
     * 荷兰
     */
    EU_Netherlands("NL"),

    /**
     * 以色列
     */
    Israel("IL"),

    /**
     * 马来西亚
     */
    Malaysia("MY"),

    /**
     * 哈萨克斯坦
     */
    Kazakhstan("KZ"),

    /**
     * 泰国
     */
    Thailand("TH"),

    /**
     * 缅甸
     */
    Myanmar("MM"),

    /**
     * 越南
     */
    VietNam("VN"),

    /**
     * 印度尼西亚
     */
    Indonesia("ID"),

    /**
     * 新西兰
     */
    NewZealand("NZ"),

    /**
     * 新加坡
     */
    Singapore("SG"),

    /**
     * 孟加拉国
     */
    Bangladesh("BD"),

    /**
     * 斯里兰卡
     */
    SriLanka("LK"),

    /**
     * 巴基斯坦
     */
    Pakistan("PK"),

    /**
     * 土库曼斯坦
     */
    Turkmenistan("TM"),
    ;

    companion object {

        /**
         * 获取国家码查找枚举
         */
        fun find(code: String): CountryCodeEnum {
            for (x in values()) {
                if (x.code == code) return x
            }
            return Other
        }
    }
}