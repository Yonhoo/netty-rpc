package com.yonhoo.nettyrpc.common;

public interface Destroyable {
    void destroy();

    interface DestroyHook {
        public void preDestroy();

        public void postDestroy();
    }
}
