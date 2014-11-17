package cs244b.dstore.api;

public class PathUtils {
    public static String normalizePath(String path) {
        if (path == null || !path.matches("^/[a-zA-Z0-9:_/]*")) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder("/");
        char last = '/';
        for (int i = 1; i < path.length(); i++) {
            char cur = path.charAt(i);
            if (last == '/' && cur == '/') {
                continue;
            }
            sb.append(cur);
            last = cur;
        }
        if (sb.length() > 1 && sb.charAt(sb.length()-1) == '/') { // not "/"
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
}
