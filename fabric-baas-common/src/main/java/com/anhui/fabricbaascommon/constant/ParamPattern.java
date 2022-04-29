package com.anhui.fabricbaascommon.constant;

public class ParamPattern {
    public static final String CHANNEL_NAME_REGEX = "^[a-z][a-z0-9]{3,}$";
    public static final String CHANNEL_NAME_MSG = "通道名称长度至少为4，只能由小写字母和数字组成";

    public static final String NETWORK_NAME_REGEX = "^[A-Za-z0-9]{4,}$";
    public static final String NETWORK_NAME_MSG = "网络名称长度至少为4，只能由字母和数字组成";

    public static final String CONSORTIUM_NAME_REGEX = "^[A-Za-z0-9]{4,}$";
    public static final String CONSORTIUM_NAME_MSG = "联盟名称长度至少为4，只能由字母和数字组成";

    public static final String ORG_NAME_REGEX = "^[A-Za-z]{3,}$";
    public static final String ORG_NAME_MSG = "组织名称只能由大小写字母组成且长度不小于4，例如WikimediaFoundationInc";

    public static final String HOST_REGEX = "^(?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$";
    public static final String HOST_MSG = "请检查域名格式是否正确";

    public static final String API_SERVER_REGEX = "^(?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+:[1-9][0-9]+$";
    public static final String API_SERVER_MSG = "服务端地址例如example.com:8080";

    public static final String COUNTRY_CODE_REGEX = "^[A-Z]{2}$";
    public static final String COUNTRY_CODE_MSG = "国家代码必须是两位大写字母，例如CN";

    public static final String STATE_OR_PROVINCE_REGEX = "^[A-Za-z]+$";
    public static final String STATE_OR_PROVINCE_MSG = "省份或州名必须是由大小写字母组成的非空字符串";

    public static final String LOCALITY_REGEX = "^[A-Za-z]+$";
    public static final String LOCALITY_MSG = "城市名必须是由大小写字母组成的非空字符串";

    public static final String NODE_NAME_REGEX = "^[A-Za-z][A-Za-z0-9]+$";
    public static final String NODE_NAME_MSG = "节点的名称只能包含大小写字母和数字（不能以数字开头且长度大于1）";

    public static final String CA_USERNAME_REGEX = "^[A-Za-z][A-Za-z0-9]+$";
    public static final String CA_USERNAME_MSG = "CA账户名称只能包含大小写字母和数字（不能以数字开头且长度大于1）";

    public static final String CA_PASSWORD_REGEX = "^[A-Za-z0-9]{8,}$";
    public static final String CA_PASSWORD_MSG = "CA账户密码只能包含大小写字母和数字（长度不小于8）";

    public static final String CA_USERTYPE_REGEX = "^(admin)|(client)|(peer)|(orderer)$";
    public static final String CA_USERTYPE_MSG = "CA账户类型只能为admin、client、peer和orderer中的一种";

    public static final String CHAINCODE_NAME_REGEX = "^[A-Za-z][A-Za-z0-9]+$";
    public static final String CHAINCODE_NAME_MSG = "链码名称只能包含大小写字母和数字（不能以数字开头且长度大于1）";

    public static final String CHAINCODE_VERSION_REGEX = "^\\d+(\\.\\d+){2}$";
    public static final String CHAINCODE_VERSION_MSG = "链码版本只能包含数字和小数点（格式例如1.0.0）";
}

