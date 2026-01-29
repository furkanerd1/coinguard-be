package com.coinguard.common.constant;

public final class RestApiPaths {

    private RestApiPaths() {}

    public static final String API_VERSION_1 = "/api/v1";

    public static final String AUTH = API_VERSION_1 + "/auth";

    public static final class Transaction {
        private Transaction() {}
        public static final String CTRL = API_VERSION_1 + "/transactions";
        public static final String TRANSFER = "/transfer";
        public static final String DEPOSIT = "/deposit";
        public static final String WITHDRAW = "/withdraw";
        public static final String HISTORY = "/history";
    }

    public static final class Budget {
        private Budget() {}
        public static final String CTRL = API_VERSION_1 + "/budgets";
    }

    public static final class Wallet {
        private Wallet() {}
        public static final String CTRL = API_VERSION_1 + "/wallets";
    }

    public static final class User {
        private User() {}
        public static final String CTRL = API_VERSION_1 + "/users";
        public static final String LOGGED_IN = "/me";
        public static final String SEARCH = "/search";
    }
}
