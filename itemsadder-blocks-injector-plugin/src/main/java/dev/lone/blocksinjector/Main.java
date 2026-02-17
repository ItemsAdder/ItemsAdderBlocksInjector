package dev.lone.blocksinjector;

public final class Main extends AbstractMain {

    @Override
    void createPacketListener(String version) {
        Nms.createPacketListener(version, this);
    }
}
