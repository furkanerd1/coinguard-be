package com.coinguard.common.constant;

public final class RestApiPaths {

    private RestApiPaths(){}

    public static final String API_VERSION_1 = "/api/v1";

    public static final class Transaction {
        public static final String CTRL = API_VERSION_1 + "/transactions";
        public static final String TRANSFER = "/transfer";
        public static final String DEPOSIT = "/deposit";
        public static final String WITHDRAW = "/withdraw";
        public static final String HISTORY = "/history";
    }
}
