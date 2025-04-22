package io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions;

import java.security.BasicPermission;
import java.security.Permission;

public class RecordBeanParamPermission extends BasicPermission {
    private final String documentId;
    private final String customAuthorizationHeader;
    private final String accessLevel;
    private final String principalName;

    public RecordBeanParamPermission(String name, String documentId, String customAuthorizationHeader,
            String accessLevel, String principalName) {
        super(name);
        this.documentId = documentId;
        this.customAuthorizationHeader = customAuthorizationHeader;
        this.accessLevel = accessLevel;
        this.principalName = principalName;
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof PossessedPermission possessedPermission) {
            String possessedPermissionName = permission.getName();
            String[] possessedPermissionActions = possessedPermission.actions;
            return getName().equals(possessedPermissionName) && accessLevel.equals(possessedPermissionActions[0]);
        }
        return false;
    }

    @Override
    public String getActions() {
        return accessLevel;
    }
}
