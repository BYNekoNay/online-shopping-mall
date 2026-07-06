package com.pzhu.mall.security;

public class LoginUserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getCurrentUserId() { return USER_ID.get(); }
    public static Integer getCurrentRole() { return ROLE.get(); }
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
