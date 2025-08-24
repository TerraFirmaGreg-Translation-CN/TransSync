package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.Config;

public interface ConfigChangeListener {
    void onConfigChanged(Config newConfig);
}