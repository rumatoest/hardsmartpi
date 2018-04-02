package hspi.app;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.undertow.UndertowModule;
import io.bootique.undertow.UndertowModuleProvider;

import java.util.Collection;
import java.util.Collections;

public class HspiModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new HspiModule();
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.singletonList(UndertowModule.class);
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return Collections.singletonList(new UndertowModuleProvider());
    }
}
