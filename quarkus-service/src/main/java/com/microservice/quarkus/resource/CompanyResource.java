package com.microservice.quarkus.resource;

import java.net.URI;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.microservice.quarkus.dto.CompanyDto;
import com.microservice.quarkus.model.Company;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class CompanyResource {
    private static final Logger log = LoggerFactory.getLogger(CompanyResource.class);

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE", "COMPANY_DELETE", "ROLE_COMPANY_CREATE"})
    public Response getAllActiveCompanies(@Context SecurityContext ctx) {
        String name = ctx.getUserPrincipal().getName();
        log.info("hello {}", name);
        return Response.ok((hasRoleAdmin(ctx) ? Company.findActiveCompanies() : Company.findActiveCompaniesByUser(name))).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE"})
    public Response getById(@PathParam("id") String id, @Context SecurityContext ctx) {
        Optional<Company> company = getCompanyById(id);
        if (company.isPresent()) {
            return company.filter(hasPermissionToChangeCompany(ctx))
                    .map(c -> Response.ok(c).build())
                    .orElseThrow(() -> new ForbiddenException(String.format("User(%s) does not have access to this resource", ctx.getUserPrincipal().getName())));
        } else {
            throw new NotFoundException();
        }
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE"})
    public Response create(CompanyDto companyDto, @Context SecurityContext ctx) {
        Company company = new Company();
        company.name = companyDto.getName();
        company.createdByUser = ctx.getUserPrincipal().getName();
        company.persist();
        return Response.created(URI.create(String.format("/api/companies/%s", company.id)))
                .entity(company)
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE"})
    public Response update(CompanyDto companyDto, @PathParam("id") String id, @Context SecurityContext ctx) {
        return getCompanyById(id)
                .map(c -> {
                    c.name = companyDto.getName();
                    c.lastModifiedByUser = ctx.getUserPrincipal().getName();
                    c.update();
                    return c;
                })
                .map(c -> Response.ok(c).build())
                .orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_DELETE"})
    public Response delete(@PathParam("id") String id, @Context SecurityContext ctx) {
        return getCompanyById(id)
                .filter(hasPermissionToChangeCompany(ctx))
                .map(c -> {
                    c.delete();
                    return Response.noContent().build();
                })
                .orElseThrow(NotFoundException::new);
    }

    private Optional<Company> getCompanyById(String id) {
        return Company.findByIdOptional(new ObjectId(id));
    }

    private boolean hasRoleAdmin(SecurityContext ctx) {
        return ctx.isUserInRole("ROLE_ADMIN");
    }

    private Predicate<Company> hasPermissionToChangeCompany(SecurityContext ctx) {
        return c -> hasRoleAdmin(ctx) || c.createdByUser.equals(ctx.getUserPrincipal().getName());
    }
}