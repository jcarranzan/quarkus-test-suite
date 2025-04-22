package io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions;

import java.security.BasicPermission;
import java.security.Permission;

public class PossessedPermission extends BasicPermission {
    final String[] actions;

    public PossessedPermission(String name, String[] actions) {
        super(name);
        this.actions = actions;
    }

    @Override
    public boolean implies(Permission p) {
        if (p instanceof SimpleBeanParamPermission
                || p instanceof NestedBeanParamPermission) {
            return p.implies(this);
        }
        return false;
    }

}