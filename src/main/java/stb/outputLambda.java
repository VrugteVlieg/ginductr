package stb;

public interface outputLambda {
    public static String CLEAR = "CLEAR";
    public static String APPEND = "APPEND";
    public static String SET = "SET";
    public static String UPDATE = "UPDATE";
    public static String PUSH = "PUSH";
    public static String POP = "POP";
    public void output(String toOutput);
}