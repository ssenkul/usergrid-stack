/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.management.organizations.applications;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.security.oauth.ClientCredentialsInfo;
import org.usergrid.security.providers.SignInAsProvider;
import org.usergrid.security.providers.SignInProviderFactory;
import org.usergrid.services.ServiceManager;

import com.sun.jersey.api.json.JSONWithPadding;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component("org.usergrid.rest.management.organizations.applications.ApplicationResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class ApplicationResource extends AbstractContextResource {

	OrganizationInfo organization;
	UUID applicationId;
	ApplicationInfo application;

  @Autowired
  private SignInProviderFactory signInProviderFactory;
  
	public ApplicationResource() {
	}

	public ApplicationResource init(OrganizationInfo organization,
			UUID applicationId) {
		this.organization = organization;
		this.applicationId = applicationId;
		return this;
	}

	public ApplicationResource init(OrganizationInfo organization,
			ApplicationInfo application) {
		this.organization = organization;
		applicationId = application.getId();
		this.application = application;
		return this;
	}

	@RequireOrganizationAccess
	@DELETE
	public JSONWithPadding deleteApplicationFromOrganizationByApplicationId(
			@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = createApiResponse();
		response.setAction("delete application from organization");

		management.deleteOrganizationApplication(organization.getUuid(),
				applicationId);

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@GET
	public JSONWithPadding getApplication(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = createApiResponse();
		ServiceManager sm = smf.getServiceManager(applicationId);
        response.setAction("get");
        response.setApplication(sm.getApplication());
        response.setParams(ui.getQueryParameters());
        response.setResults(management.getApplicationMetadata(applicationId));
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@GET
	@Path("credentials")
	public JSONWithPadding getCredentials(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = createApiResponse();
		response.setAction("get application client credentials");

		ClientCredentialsInfo credentials = new ClientCredentialsInfo(
				management.getClientIdForApplication(applicationId),
				management.getClientSecretForApplication(applicationId));

		response.setCredentials(credentials);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Path("credentials")
	public JSONWithPadding generateCredentials(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = createApiResponse();
		response.setAction("generate application client credentials");

		ClientCredentialsInfo credentials = new ClientCredentialsInfo(
				management.getClientIdForApplication(applicationId),
				management.newClientSecretForApplication(applicationId));

		response.setCredentials(credentials);
		return new JSONWithPadding(response, callback);
	}

  @POST
  @Path("sia-provider")
  @Consumes(APPLICATION_JSON)
  @RequireOrganizationAccess
  public JSONWithPadding configureProvider(@Context UriInfo ui,
                                           @QueryParam("provider_key") String siaProvider,
                                           Map<String, Object> json,
                                           @QueryParam("callback") @DefaultValue("") String callback)
          throws Exception {

    ApiResponse response = createApiResponse();
    response.setAction("post signin provider configuration");

    Preconditions.checkArgument(siaProvider != null, "Sign in provider required");

    SignInAsProvider signInAsProvider = null;
    if (StringUtils.equalsIgnoreCase(siaProvider, "facebook")) {
      signInAsProvider = signInProviderFactory.facebook(smf.getServiceManager(applicationId).getApplication());
    } else if ( StringUtils.equalsIgnoreCase(siaProvider, "pingident") ) {
      signInAsProvider = signInProviderFactory.pingident(smf.getServiceManager(applicationId).getApplication());
    }else if ( StringUtils.equalsIgnoreCase(siaProvider, "foursquare") ) {
      signInAsProvider = signInProviderFactory.foursquare(smf.getServiceManager(applicationId).getApplication());
    }

    Preconditions.checkArgument(signInAsProvider != null, "No signin provider found by that name: " + siaProvider);

    signInAsProvider.saveToConfiguration(json);

    return new JSONWithPadding(response, callback);
  }

}
