package io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions;

import java.security.BasicPermission;
import java.security.Permission;

public class SimpleBeanParamPermission extends BasicPermission {

    private final String resourceId;
    private final String customAuthorizationHeader;
    private final String action;
    private final String principalName;

    public SimpleBeanParamPermission(String name, String resourceId, String customAuthorizationHeader,
            String action, String principalName) {
        super(name);
        this.resourceId = resourceId;
        this.customAuthorizationHeader = customAuthorizationHeader;
        this.action = action;
        this.principalName = principalName;
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof PossessedPermission possessedPermission) {
            String possessedPermissionName = permission.getName();
            String[] possessedPermissionActions = possessedPermission.actions;
            return getName().equals(possessedPermissionName) && action.equals(possessedPermissionActions[0]);
        }
        return false;
    }

    @Override
    public String getActions() {
        return action;
    }
}
