public final class Constants {
    private Constants() {}
    
    public static final class UI {
        public static final int DEFAULT_PADDING = 10;
        public static final int DEFAULT_SPACING = 5;
        public static final int DEFAULT_BUTTON_WIDTH = 100;
        public static final int DEFAULT_BUTTON_HEIGHT = 30;
    }
    
    public static final class Database {
        public static final String JDBC_PREFIX = "jdbc:mysql://";
        public static final String DEFAULT_PARAMS = 
            "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
    
    public static final class Messages {
        public static final String CONNECTION_SUCCESS = "Connection successful!";
        public static final String CONNECTION_FAILED = "Connection failed: %s";
    }
} 