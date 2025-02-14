/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.BaseUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.GeneralUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SelectiveUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseLinkDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedRolesDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIdList;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.OrganizationScope;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.API_REF_GET_SHARED_ROLES_OF_USER_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_GENERAL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_SELECTIVE_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_SKIP_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Implementation of the user sharing policy handler service.
 */
public class UserSharingPolicyHandlerServiceImpl implements UserSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImpl.class);
    private final UserIDResolver userIDResolver = new UserIDResolver();

    @Override
    public void populateSelectiveUserShare(SelectiveUserShareDO selectiveUserShareDO) throws UserSharingMgtException {

        validateUserShareInput(selectiveUserShareDO);

        List<SelectiveUserShareOrgDetailsDO> organizations = selectiveUserShareDO.getOrganizations();
        Map<String, UserCriteriaType> userCriteria = selectiveUserShareDO.getUserCriteria();

        String sharingInitiatedOrgId = getOrganizationId();
        List<String> immediateChildOrgs = getChildOrgsOfSharingInitiatedOrg(sharingInitiatedOrgId);

        for (SelectiveUserShareOrgDetailsDO organization : organizations) {
            if (immediateChildOrgs.contains(organization.getOrganizationId())) {
                populateSelectiveUserShareByCriteria(organization, userCriteria);
            } else {
                throw new UserSharingMgtClientException(ERROR_SKIP_SHARE);
            }
        }
        LOG.debug("Completed user selective share initiated from " + getOrganizationManager() + ".");
    }

    @Override
    public void populateGeneralUserShare(GeneralUserShareDO generalUserShareDO) throws UserSharingMgtException {

        validateUserShareInput(generalUserShareDO);

        Map<String, UserCriteriaType> userCriteria = generalUserShareDO.getUserCriteria();
        PolicyEnum policy = generalUserShareDO.getPolicy();
        List<String> roleIds = getRoleIds(generalUserShareDO.getRoles());

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIdList) {
                        generalUserShareByUserIds((UserIdList) criterionValues, policy, roleIds);
                    } else {
                        throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
                    }
                    break;
                default:
                    throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
            }
        }
        LOG.debug("Completed user general share initiated from " + getOrganizationManager() + ".");
    }

    @Override
    public void populateSelectiveUserUnshare(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtException {

        validateUserUnshareInput(selectiveUserUnshareDO);

        Map<String, UserCriteriaType> userCriteria = selectiveUserUnshareDO.getUserCriteria();
        List<String> organizations = selectiveUserUnshareDO.getOrganizations();

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIdList) {
                        selectiveUserUnshareByUserIds((UserIdList) criterionValues, organizations);
                    } else {
                        throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
                    }
                    break;
                default:
                    throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
            }
        }
        LOG.debug("Completed user selective unshare initiated from " + getOrganizationManager() + ".");
    }

    @Override
    public void populateGeneralUserUnshare(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException {

        validateUserUnshareInput(generalUserUnshareDO);

        Map<String, UserCriteriaType> userCriteria = generalUserUnshareDO.getUserCriteria();

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIdList) {
                        generalUserUnshareByUserIds((UserIdList) criterionValues);
                    } else {
                        throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
                    }
                    break;
                default:
                    throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
            }
        }
        LOG.debug("Completed user general unshare initiated from " + getOrganizationManager() + ".");
    }

    @Override
    public ResponseSharedOrgsDO getSharedOrganizationsOfUser(String associatedUserId, String after, String before,
                                                             Integer limit, String filter, Boolean recursive)
            throws UserSharingMgtException {

        try {
            List<ResponseOrgDetailsDO> responseOrgDetailsDOS = new ArrayList<>();
            List<ResponseLinkDO> responseLinkList = Collections.singletonList(new ResponseLinkDO());
            List<UserAssociation> userAssociations =
                    getOrganizationUserSharingService().getUserAssociationsOfGivenUser(associatedUserId,
                            getOrganizationId());

            for (UserAssociation userAssociation : userAssociations) {
                ResponseOrgDetailsDO responseOrgDetailsDO = new ResponseOrgDetailsDO();
                responseOrgDetailsDO.setOrganizationId(userAssociation.getOrganizationId());
                responseOrgDetailsDO.setOrganizationName(getOrganizationName(userAssociation.getOrganizationId()));
                responseOrgDetailsDO.setSharedUserId(userAssociation.getUserId());
                responseOrgDetailsDO.setSharedType(userAssociation.getSharedType());
                responseOrgDetailsDO.setRolesRef(getRolesRef(associatedUserId, userAssociation.getOrganizationId()));
                responseOrgDetailsDOS.add(responseOrgDetailsDO);
            }

            return new ResponseSharedOrgsDO(responseLinkList, responseOrgDetailsDOS);
        } catch (OrganizationManagementException e) {
            throw new UserSharingMgtClientException(ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER);
        }
    }

    @Override
    public ResponseSharedRolesDO getRolesSharedWithUserInOrganization(String associatedUserId, String orgId,
                                                                      String after, String before, Integer limit,
                                                                      String filter, Boolean recursive)
            throws UserSharingMgtException {

        try {
            List<RoleWithAudienceDO> roleWithAudienceList = new ArrayList<>();
            List<ResponseLinkDO> responseLinkList = Collections.singletonList(new ResponseLinkDO());
            UserAssociation userAssociation =
                    getOrganizationUserSharingService().getUserAssociationOfAssociatedUserByOrgId(associatedUserId,
                            orgId);

            if (userAssociation == null) {
                return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
            }

            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

            String usernameWithDomain = userIDResolver.getNameByID(userAssociation.getUserId(), tenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            List<String> sharedRoleIdsInOrg =
                    getOrganizationUserSharingService().getRolesSharedWithUserInOrganization(username, tenantId,
                            domainName);

            if (CollectionUtils.isEmpty(sharedRoleIdsInOrg)) {
                return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
            }

            RoleManagementService roleManagementService = getRoleManagementService();

            for (String sharedRoleId : sharedRoleIdsInOrg) {
                Role role = roleManagementService.getRole(sharedRoleId, tenantDomain);
                RoleWithAudienceDO roleWithAudience = new RoleWithAudienceDO();
                roleWithAudience.setRoleName(role.getName());
                roleWithAudience.setAudienceName(role.getAudienceName());
                roleWithAudience.setAudienceType(role.getAudience());
                roleWithAudienceList.add(roleWithAudience);
            }

            return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new UserSharingMgtClientException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER);
        }
    }

    private void populateSelectiveUserShareByCriteria(SelectiveUserShareOrgDetailsDO organization,
                                                      Map<String, UserCriteriaType> userCriteria)
            throws UserSharingMgtException {

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIdList) {
                        selectiveUserShareByUserIds((UserIdList) criterionValues, organization);
                    } else {
                        throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
                    }
                    break;
                default:
                    throw new UserSharingMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID);
            }
        }
    }

    private void selectiveUserShareByUserIds(UserIdList userIds, SelectiveUserShareOrgDetailsDO organization)
            throws UserSharingMgtException {

        String sharingInitiatedOrgId = getOrganizationId();
        for (String associatedUserId : userIds.getIds()) {
            try {
                SelectiveUserShare selectiveUserShare = new SelectiveUserShare.Builder()
                        .withUserId(associatedUserId)
                        .withOrganizationId(organization.getOrganizationId())
                        .withPolicy(organization.getPolicy())
                        .withRoles(getRoleIds(organization.getRoles()))
                        .build();
                shareUser(selectiveUserShare, sharingInitiatedOrgId);
            } catch (OrganizationManagementException | IdentityRoleManagementException |
                     ResourceSharingPolicyMgtException e) {
                String errorMessage =
                        String.format(ERROR_SELECTIVE_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_SELECTIVE_SHARE, errorMessage);
            }
        }
        LOG.debug("Completed user selective share.");
    }

    private void shareUser(BaseUserShare userShare, String sharingInitiatedOrgId)
            throws OrganizationManagementException, UserSharingMgtException, IdentityRoleManagementException,
            ResourceSharingPolicyMgtException {

        List<String> orgsToShareUserWith = getOrgsToShareUserWithBasedOnSharingType(userShare, sharingInitiatedOrgId);

        if (isUserAlreadyShared(userShare.getUserId(), sharingInitiatedOrgId)) {
            handleExistingSharedUser(userShare, sharingInitiatedOrgId, orgsToShareUserWith);
        } else {
            createNewUserShare(userShare, sharingInitiatedOrgId, orgsToShareUserWith);
        }
    }

    private List<String> getOrgsToShareUserWithBasedOnSharingType(BaseUserShare userShare,
                                                                  String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        if (userShare instanceof SelectiveUserShare) {
            return getOrgsToShareUserWith(((SelectiveUserShare) userShare).getOrganizationId(), userShare.getPolicy());
        }
        return getOrgsToShareUserWith(sharingInitiatedOrgId, userShare.getPolicy());
    }

    private void handleExistingSharedUser(BaseUserShare userShare, String sharingInitiatedOrgId,
                                          List<String> orgsToShareUserWith)
            throws UserSharingMgtException, IdentityRoleManagementException, OrganizationManagementException,
            ResourceSharingPolicyMgtException {

        List<UserAssociation> userAssociations =
                getSharedUserAssociationsOfGivenUser(userShare.getUserId(), sharingInitiatedOrgId);
        List<String> retainedSharedOrganizations = new ArrayList<>();

        for (UserAssociation association : userAssociations) {
            if (!orgsToShareUserWith.contains(association.getOrganizationId())) {
                unshareUserFromPreviousOrg(association);
            } else {
                retainedSharedOrganizations.add(association.getOrganizationId());
                updateRolesIfNecessary(association, userShare.getRoles(), sharingInitiatedOrgId);
            }
        }

        shareWithNewOrganizations(userShare, sharingInitiatedOrgId, orgsToShareUserWith, retainedSharedOrganizations);
        updateResourceSharingPolicy(userShare, sharingInitiatedOrgId);
    }

    private void unshareUserFromPreviousOrg(UserAssociation association) throws UserSharingMgtException {

        selectiveUserUnshareByUserIds(new UserIdList(Collections.singletonList(association.getAssociatedUserId())),
                Collections.singletonList(association.getOrganizationId()));
    }

    private void shareWithNewOrganizations(BaseUserShare userShare, String sharingInitiatedOrgId,
                                           List<String> orgsToShareUserWith, List<String> alreadySharedOrgs)
            throws OrganizationManagementException, UserSharingMgtException, IdentityRoleManagementException {

        List<String> newlySharedOrgs = new ArrayList<>(orgsToShareUserWith);
        newlySharedOrgs.removeAll(alreadySharedOrgs);

        for (String orgId : newlySharedOrgs) {
            shareAndAssignRolesIfPresent(orgId, userShare, sharingInitiatedOrgId);
        }
    }

    private void createNewUserShare(BaseUserShare userShare, String sharingInitiatedOrgId,
                                    List<String> orgsToShareUserWith)
            throws OrganizationManagementException, UserSharingMgtException, IdentityRoleManagementException,
            ResourceSharingPolicyMgtException {

        for (String orgId : orgsToShareUserWith) {
            shareAndAssignRolesIfPresent(orgId, userShare, sharingInitiatedOrgId);
        }

        if (isApplicableOrganizationScopeForSavingPolicy(userShare.getPolicy())) {
            saveUserSharingPolicy(userShare, sharingInitiatedOrgId);
        }
    }

    private List<String> getChildOrgsOfSharingInitiatedOrg(String sharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        try {
            return getOrganizationManager().getChildOrganizationsIds(getOrganizationId(), false);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(
                    ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS.getMessage(), sharingInitiatedOrgId);
            throw new UserSharingMgtServerException(ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS, errorMessage);
        }
    }

    private void generalUserShareByUserIds(UserIdList userIds, PolicyEnum policy, List<String> roleIds)
            throws UserSharingMgtException {

        String sharingInitiatedOrgId = getOrganizationId();
        for (String associatedUserId : userIds.getIds()) {
            try {
                GeneralUserShare generalUserShare = new GeneralUserShare.Builder()
                        .withUserId(associatedUserId)
                        .withPolicy(policy)
                        .withRoles(roleIds)
                        .build();

                shareUser(generalUserShare, sharingInitiatedOrgId);
            } catch (OrganizationManagementException | IdentityRoleManagementException |
                     ResourceSharingPolicyMgtException e) {
                String errorMessage = String.format(ERROR_GENERAL_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_GENERAL_SHARE, errorMessage);
            }
        }

        LOG.debug("Completed user general share.");
    }

    private boolean hasRoleChanges(List<String> oldSharedRoleIds, List<String> newRoleIds) {

        return !new HashSet<>(oldSharedRoleIds).equals(new HashSet<>(newRoleIds));
    }

    private void updateRolesIfNecessary(UserAssociation userAssociation, List<String> roleIds,
                                        String sharingInitiatedOrgId)
            throws OrganizationManagementException, IdentityRoleManagementException, UserSharingMgtException {

        List<String> currentSharedRoleIds = getCurrentSharedRoleIdsForSharedUser(userAssociation);
        List<String> newSharedRoleIds = getRolesToBeAddedAfterUpdate(userAssociation, currentSharedRoleIds, roleIds);

        if (hasRoleChanges(currentSharedRoleIds, newSharedRoleIds)) {
            assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, newSharedRoleIds);
        }
    }

    private List<String> getCurrentSharedRoleIdsForSharedUser(UserAssociation userAssociation)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        List<String> allUserRolesOfSharedUser = getRoleManagementService().getRoleIdListOfUser(userId, tenantDomain);

        return getOrganizationUserSharingService().getSharedUserRolesFromUserRoles(allUserRolesOfSharedUser,
                tenantDomain);
    }

    private void updateResourceSharingPolicy(BaseUserShare baseUserShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        //Delete old sharing policy.
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                sharingInitiatedOrgId, ResourceType.USER, baseUserShare.getUserId(), sharingInitiatedOrgId);

        //Create new sharing policy.
        if (isApplicableOrganizationScopeForSavingPolicy(baseUserShare.getPolicy())) {
            saveUserSharingPolicy(baseUserShare, sharingInitiatedOrgId);
        }
    }

    private void saveUserSharingPolicy(BaseUserShare userShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        ResourceSharingPolicy resourceSharingPolicy =
                new ResourceSharingPolicy.Builder().withResourceType(ResourceType.USER)
                        .withResourceId(userShare.getUserId())
                        .withInitiatingOrgId(sharingInitiatedOrgId)
                        .withPolicyHoldingOrgId(getPolicyHoldingOrgId(userShare, sharingInitiatedOrgId))
                        .withSharingPolicy(userShare.getPolicy()).build();

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        for (String roleId : userShare.getRoles()) {
            SharedResourceAttribute sharedResourceAttribute =
                    new SharedResourceAttribute.Builder().withSharedAttributeType(SharedAttributeType.ROLE)
                            .withSharedAttributeId(roleId).build();
            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                sharedResourceAttributes);

    }

    /**
     * Determines the policy-holding organization ID based on the type of user share.
     * For a selective user share, the policy-holding organization is the organization specified in the selective
     * share request.
     * For a general user share, the policy-holding organization is the organization from which the
     * sharing request was initiated.
     *
     * @param userShare             The user share object, which can be either selective or general.
     * @param sharingInitiatedOrgId The ID of the organization from which the sharing request was initiated.
     * @return The ID of the policy-holding organization based on the type of user share.
     */
    private String getPolicyHoldingOrgId(BaseUserShare userShare, String sharingInitiatedOrgId) {

        if (userShare instanceof SelectiveUserShare) {
            return ((SelectiveUserShare) userShare).getOrganizationId();
        } else {
            return sharingInitiatedOrgId;
        }
    }

    private boolean isApplicableOrganizationScopeForSavingPolicy(PolicyEnum policy) {

        return OrganizationScope.EXISTING_ORGS_AND_FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope()) ||
                OrganizationScope.FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope());
    }

    private void shareAndAssignRolesIfPresent(String orgId, BaseUserShare baseUserShare,
                                              String sharingInitiatedOrgId)
            throws OrganizationManagementException, IdentityRoleManagementException, UserSharingMgtException {

        String associatedUserId = baseUserShare.getUserId();
        List<String> roleIds = baseUserShare.getRoles();
        UserAssociation userAssociation;

        try {
            userAssociation = shareUserWithOrganization(orgId, associatedUserId, sharingInitiatedOrgId);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(ERROR_CODE_USER_SHARE.getMessage(), associatedUserId, e.getMessage());
            LOG.error(errorMessage, e);
            return;
        }

        //Assign roles if any are present.
        assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, roleIds);
    }

    private boolean isUserAlreadyShared(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        List<UserAssociation> userAssociationsOfGivenUser =
                getSharedUserAssociationsOfGivenUser(associatedUserId, associatedOrgId);

        return userAssociationsOfGivenUser != null && !userAssociationsOfGivenUser.isEmpty();
    }

    private List<UserAssociation> getSharedUserAssociationsOfGivenUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        return getOrganizationUserSharingService().getUserAssociationsOfGivenUser(associatedUserId, associatedOrgId,
                SharedType.SHARED);
    }

    private List<String> getRolesToBeAddedAfterUpdate(UserAssociation userAssociation, List<String> currentRoleIds,
                                                      List<String> newRoleIds)
            throws OrganizationManagementException, IdentityRoleManagementException {

        // Roles to be added are those in newRoleIds that are not in currentRoleIds.
        List<String> rolesToBeAdded = new ArrayList<>(newRoleIds);
        rolesToBeAdded.removeAll(currentRoleIds);

        // Roles to be removed are those in currentRoleIds that are not in newRoleIds.
        List<String> rolesToBeRemoved = new ArrayList<>(currentRoleIds);
        rolesToBeRemoved.removeAll(newRoleIds);

        deleteOldSharedRoles(userAssociation, rolesToBeRemoved);
        return rolesToBeAdded;
    }

    private void deleteOldSharedRoles(UserAssociation userAssociation, List<String> rolesToBeRemoved)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        for (String roleId : rolesToBeRemoved) {
            getRoleManagementService().updateUserListOfRole(roleId, Collections.emptyList(),
                    Collections.singletonList(userId), targetOrgTenantDomain);
        }
    }

    private void assignRolesIfPresent(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                      List<String> roleIds)
            throws OrganizationManagementException, IdentityRoleManagementException, UserSharingMgtException {

        if (!roleIds.isEmpty()) {
            assignRolesToTheSharedUser(userAssociation, sharingInitiatedOrgId, roleIds);
        }
    }

    private void assignRolesToTheSharedUser(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                            List<String> roleIds)
            throws OrganizationManagementException, IdentityRoleManagementException, UserSharingMgtException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String sharingInitiatedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        String usernameWithDomain = userIDResolver.getNameByID(userId, targetOrgTenantDomain);
        String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
        String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

        RoleManagementService roleManagementService = getRoleManagementService();
        Map<String, String> sharedRoleToMainRoleMappingsBySubOrg =
                roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(roleIds,
                        sharingInitiatedOrgTenantDomain);

        List<String> mainRoles = new ArrayList<>();
        for (String roleId : roleIds) {
            mainRoles.add(sharedRoleToMainRoleMappingsBySubOrg.getOrDefault(roleId, roleId));
        }

        Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainRoles, targetOrgTenantDomain);

        for (String role : mainRoleToSharedRoleMappingsBySubOrg.values()) {
            roleManagementService.updateUserListOfRole(role, Collections.singletonList(userId),
                    Collections.emptyList(), targetOrgTenantDomain);
            roleManagementService.getRoleListOfUser(userId, targetOrgTenantDomain);

            getOrganizationUserSharingService().addEditRestrictionsForSharedUserRole(role, username,
                    targetOrgTenantDomain, domainName, EditOperation.DELETE, sharingInitiatedOrgId);
        }
    }

    private UserAssociation shareUserWithOrganization(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        OrganizationUserSharingService organizationUserSharingService = getOrganizationUserSharingService();
        organizationUserSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId,
                SharedType.SHARED);
        return organizationUserSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
    }

    private List<String> getOrgsToShareUserWith(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> orgsToShareUserWith = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_ORGS_ONLY:
            case ALL_EXISTING_AND_FUTURE_ORGS:
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case IMMEDIATE_EXISTING_ORGS_ONLY:
            case IMMEDIATE_EXISTING_AND_FUTURE_ORGS:
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case SELECTED_ORG_ONLY:
                orgsToShareUserWith.add(policyHoldingOrgId);
                break;

            case SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                orgsToShareUserWith.add(policyHoldingOrgId);
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN:
                orgsToShareUserWith.add(policyHoldingOrgId);
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case NO_SHARING:
                break;

            default:
                throw new OrganizationManagementClientException(
                        String.format(ERROR_CODE_INVALID_POLICY.getMessage(), policy.getPolicyName()),
                        ERROR_CODE_INVALID_POLICY.getDescription(),
                        ERROR_CODE_INVALID_POLICY.getCode());
        }

        return new ArrayList<>(orgsToShareUserWith);
    }

    private List<String> getRoleIds(List<RoleWithAudienceDO> rolesWithAudience) throws UserSharingMgtException {

        try {
            String sharingInitiatedOrgId = getOrganizationId();
            String sharingInitiatedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);

            List<String> list = new ArrayList<>();
            for (RoleWithAudienceDO roleWithAudienceDO : rolesWithAudience) {
                String audienceId =
                        getAudienceId(roleWithAudienceDO, sharingInitiatedOrgId, sharingInitiatedTenantDomain);
                Optional<String> roleId =
                        getRoleIdFromAudience(roleWithAudienceDO.getRoleName(), roleWithAudienceDO.getAudienceType(),
                                audienceId, sharingInitiatedTenantDomain);
                if (!roleId.isPresent()) {
                    continue;
                }
                list.add(roleId.get());
            }
            return list;
        } catch (OrganizationManagementException e) {
            throw new UserSharingMgtServerException(ERROR_CODE_GET_ROLE_IDS);
        }
    }

    private String getAudienceId(RoleWithAudienceDO role, String originalOrgId, String tenantDomain) {

        if (role == null || role.getAudienceType() == null) {
            return null;
        }

        try {
            if (StringUtils.equals(ORGANIZATION, role.getAudienceType())) {
                return originalOrgId;
            }
            if (StringUtils.equals(APPLICATION, role.getAudienceType())) {
                return getApplicationResourceId(role.getAudienceName(), tenantDomain);
            }
            LOG.warn(String.format(ERROR_CODE_INVALID_AUDIENCE_TYPE.getDescription(), role.getAudienceType()));
        } catch (IdentityApplicationManagementException e) {
            LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), role.getAudienceName()));
        }
        return null;
    }

    private String getApplicationResourceId(String audienceName, String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationBasicInfo applicationBasicInfo = getApplicationManagementService()
                .getApplicationBasicInfoByName(audienceName, tenantDomain);

        if (applicationBasicInfo != null) {
            return applicationBasicInfo.getApplicationResourceId();
        }
        LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), audienceName));
        return null;
    }

    private Optional<String> getRoleIdFromAudience(String roleName, String audienceType, String audienceId,
                                                   String tenantDomain) {

        if (audienceId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    getRoleManagementService().getRoleIdByName(roleName, audienceType, audienceId, tenantDomain));
        } catch (IdentityRoleManagementException e) {
            LOG.warn(String.format(ERROR_CODE_ROLE_NOT_FOUND.getMessage(), roleName, audienceType, audienceId));
            return Optional.empty();
        }
    }

    private void selectiveUserUnshareByUserIds(UserIdList userIds, List<String> organizations)
            throws UserSharingMgtServerException {

        String unsharingInitiatedOrgId = getOrganizationId();

        for (String associatedUserId : userIds.getIds()) {
            try {
                for (String organizationId : organizations) {

                    getOrganizationUserSharingService().unshareOrganizationUserInSharedOrganization(associatedUserId,
                            organizationId);

                    //Delete resource sharing policy if it has been stored for future shares.
                    deleteResourceSharingPolicyIfAny(organizationId, associatedUserId, unsharingInitiatedOrgId);

                    LOG.debug("Completed user selective unshare for associated user id : " + associatedUserId +
                            " in shared org id : " + organizationId);

                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
        }
    }

    private void deleteResourceSharingPolicyIfAny(String organizationId, String associatedUserId,
                                                  String unsharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                organizationId, ResourceType.USER, associatedUserId, unsharingInitiatedOrgId);
    }

    private String getOrganizationName(String organizationId) throws OrganizationManagementException {

        return getOrganizationManager().getOrganizationNameById(organizationId);
    }

    private String getRolesRef(String userId, String orgId) {

        return String.format(API_REF_GET_SHARED_ROLES_OF_USER_IN_ORG, userId, orgId);
    }

    private void generalUserUnshareByUserIds(UserIdList userIds)
            throws UserSharingMgtServerException {

        String unsharingInitiatedOrgId = getOrganizationId();

        for (String associatedUserId : userIds.getIds()) {
            try {
                getOrganizationUserSharingService().unshareOrganizationUsers(associatedUserId, unsharingInitiatedOrgId);

                //Delete resource sharing policy if it has been stored for future shares.
                getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(
                        ResourceType.USER, associatedUserId, unsharingInitiatedOrgId);
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed user general unshare for associated user id : " + associatedUserId);
            }
        }
    }

    //Validation methods.

    private <T extends UserCriteriaType> void validateUserShareInput(BaseUserShareDO<T> userShareDO)
            throws UserSharingMgtClientException {

        if (userShareDO == null) {
            throwValidationException(ERROR_CODE_NULL_SHARE);
        }

        if (userShareDO instanceof SelectiveUserShareDO) {
            validateSelectiveUserShareDO((SelectiveUserShareDO) userShareDO);
        } else if (userShareDO instanceof GeneralUserShareDO) {
            validateGeneralUserShareDO((GeneralUserShareDO) userShareDO);
        }
    }

    private void validateSelectiveUserShareDO(SelectiveUserShareDO selectiveUserShareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(selectiveUserShareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!selectiveUserShareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserShareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // Validate organizations list is not null.
        validateNotNull(selectiveUserShareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        // Validate each organization in the list.
        for (SelectiveUserShareOrgDetailsDO orgDetails : selectiveUserShareDO.getOrganizations()) {
            validateNotNull(orgDetails.getOrganizationId(), ERROR_CODE_ORG_ID_NULL);
            validateNotNull(orgDetails.getPolicy(), ERROR_CODE_POLICY_NULL);

            // Validate roles list is not null (it can be empty).
            if (orgDetails.getRoles() == null) {
                throwValidationException(ERROR_CODE_ROLES_NULL);
            } else {
                // Validate each role's properties if present.
                for (RoleWithAudienceDO role : orgDetails.getRoles()) {
                    validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
                    validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
                    validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
                }
            }
        }
    }

    private void validateGeneralUserShareDO(GeneralUserShareDO generalDO) throws UserSharingMgtClientException {

        validateNotNull(generalDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!generalDO.getUserCriteria().containsKey(USER_IDS) || generalDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(generalDO.getPolicy(), ERROR_CODE_POLICY_NULL);
        validateNotNull(generalDO.getRoles(), ERROR_CODE_ROLES_NULL);

        // Validate each role's properties if present.
        for (RoleWithAudienceDO role : generalDO.getRoles()) {
            validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
            validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
            validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
        }
    }

    private <T extends UserCriteriaType> void validateUserUnshareInput(BaseUserUnshareDO<T> userUnshareDO)
            throws UserSharingMgtClientException {

        if (userUnshareDO == null) {
            throwValidationException(ERROR_CODE_NULL_UNSHARE);
        }

        if (userUnshareDO instanceof SelectiveUserUnshareDO) {
            validateSelectiveUserUnshareDO((SelectiveUserUnshareDO) userUnshareDO);
        } else if (userUnshareDO instanceof GeneralUserUnshareDO) {
            validateGeneralUserUnshareDO((GeneralUserUnshareDO) userUnshareDO);
        }
    }

    private void validateSelectiveUserUnshareDO(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(selectiveUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!selectiveUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // Validate organizations list is not null.
        validateNotNull(selectiveUserUnshareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (String organization : selectiveUserUnshareDO.getOrganizations()) {
            validateNotNull(organization, ERROR_CODE_ORG_ID_NULL);
        }
    }

    private void validateGeneralUserUnshareDO(GeneralUserUnshareDO generalUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(generalUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!generalUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
    }

    private void validateNotNull(Object obj, UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        if (obj == null) {
            throwValidationException(error);
        }
    }

    private void throwValidationException(UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        throw new UserSharingMgtClientException(error.getCode(), error.getMessage(), error.getDescription());
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }
}
