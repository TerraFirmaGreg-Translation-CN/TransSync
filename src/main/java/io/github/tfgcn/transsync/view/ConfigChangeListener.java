package io.github.tfgcn.transsync.view;

import io.github.tfgcn.transsync.Config;

public interface ConfigChangeListener {
    void onConfigChanged(Config newConfig);
}