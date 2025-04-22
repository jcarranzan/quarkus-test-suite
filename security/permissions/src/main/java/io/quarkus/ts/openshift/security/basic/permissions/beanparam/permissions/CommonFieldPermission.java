package io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions;

import java.security.BasicPermission;
import java.security.Permission;

public class CommonFieldPermission extends BasicPermission {

    private final String customAuthorizationHeader;
    private final String principalName;

    public CommonFieldPermission(String name, String customAuthorizationHeader, String principalName) {
        super(name);
        this.customAuthorizationHeader = customAuthorizationHeader;
        this.principalName = principalName;
    }

    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof CommonFieldPermission)) {
            return false;
        }

        return customAuthorizationHeader != null && !customAuthorizationHeader.isEmpty();
    }

    @Override
    public String getActions() {
        return "";
    }
}
