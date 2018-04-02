package hspi.app;

import io.bootique.Bootique;

public class Application {

    public static void main(String[] args) {

        Bootique
            .app(args)
            .args("--server", "--config=classpath:hspi.yml")
            .module(new HspiModuleProvider())
            .autoLoadModules()
            .exec()
            .exit();
    }
}
