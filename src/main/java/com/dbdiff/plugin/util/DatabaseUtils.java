public final class DatabaseUtils {
    private DatabaseUtils() {}
    
    public static String formatJdbcUrl(String url) {
        if (!url.toLowerCase().startsWith(Constants.Database.JDBC_PREFIX)) {
            url = Constants.Database.JDBC_PREFIX + url;
        }
        
        if (!url.contains("?")) {
            return url + "?" + Constants.Database.DEFAULT_PARAMS;
        }
        
        if (!url.endsWith("&") && !url.endsWith("?")) {
            return url + "&" + Constants.Database.DEFAULT_PARAMS;
        }
        
        return url + Constants.Database.DEFAULT_PARAMS;
    }
    
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
} 