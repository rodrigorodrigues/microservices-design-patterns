package com.microservice.quarkus.resource;

import com.microservice.quarkus.dto.CompanyDto;
import com.microservice.quarkus.mapper.CompanyMapper;
import com.microservice.quarkus.model.Company;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.function.Predicate;

@Path("/api/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class CompanyResource {
    private static final Logger log = LoggerFactory.getLogger(CompanyResource.class);

    @Inject
    CompanyMapper companyMapper;

    @GET
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE", "COMPANY_DELETE", "ROLE_COMPANY_CREATE", "SCOPE_openid"})
    public Page<CompanyDto> getAllActiveCompanies(@Context SecurityContext ctx,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("10") Integer size) {
        io.quarkus.panache.common.Page pageRequest = io.quarkus.panache.common.Page.of(page, size);
        String name = ctx.getUserPrincipal().getName();
        log.debug("hello {}", name);
        return companyMapper.toResource(hasRoleAdmin(ctx) ? Company.findActiveCompanies(pageRequest) : Company
                .findActiveCompaniesByUser(pageRequest, name), pageRequest);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE", "SCOPE_openid"})
    public Response getById(@PathParam("id") String id, @Context SecurityContext ctx) {
        Company company = getCompanyById(id);
        if (hasPermissionToChangeCompany(ctx).test(company)) {
            return Response.ok(companyMapper.toResource(company)).build();
        } else {
            throw new ForbiddenException(String.format("User(%s) does not have access to this resource", ctx.getUserPrincipal().getName()));
        }
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE", "SCOPE_openid"})
    public Response create(@Valid CompanyDto companyDto, @Context SecurityContext ctx) {
        Company company = companyMapper.toModel(companyDto);
        company.createdByUser = ctx.getUserPrincipal().getName();
        company.persist();
        company = Company.findById(company.id);
        return Response.created(URI.create(String.format("/api/companies/%s", company.id)))
                        .entity(companyMapper.toResource(company))
                        .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE", "SCOPE_openid"})
    public Response update(@Valid CompanyDto companyDto, @PathParam("id") String id, @Context SecurityContext ctx) {
        Company c = getCompanyById(id);
        c.name = companyDto.getName();
        c.lastModifiedByUser = ctx.getUserPrincipal().getName();
        c.update();
        return Response.ok(c).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_DELETE", "SCOPE_openid"})
    public Response delete(@PathParam("id") String id, @Context SecurityContext ctx) {
        Company company = getCompanyById(id);
        if (hasPermissionToChangeCompany(ctx).test(company)) {
            company.delete();
            return Response.noContent().build();
        } else {
            throw new ForbiddenException(String.format("User(%s) does not have access to delete this resource", ctx.getUserPrincipal().getName()));
        }
    }

    private Company getCompanyById(String id) {
        return (Company) Company.findByIdOptional(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Not found id: "+id));
    }

    private boolean hasRoleAdmin(SecurityContext ctx) {
        return ctx.isUserInRole("ROLE_ADMIN");
    }

    private Predicate<Company> hasPermissionToChangeCompany(SecurityContext ctx) {
        return c -> hasRoleAdmin(ctx) || c.createdByUser.equals(ctx.getUserPrincipal().getName());
    }

}