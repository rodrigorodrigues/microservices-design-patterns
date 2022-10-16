package com.microservice.quarkus.resource;

import com.microservice.quarkus.dto.CompanyDto;
import com.microservice.quarkus.model.Company;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;
import java.util.function.Predicate;

@Path("/api/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class CompanyResource {
    private static final Logger log = LoggerFactory.getLogger(CompanyResource.class);

    @Inject
    CompanyMapper companyMapper;

/*
    private final PublishSubject<OffsetDateTime> publisher = PublishSubject.create();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public CompanyResource() {
        executorService.scheduleAtFixedRate(() -> publisher.onNext(OffsetDateTime.now()), 0, 1, TimeUnit.SECONDS);
    }

    @GET
    @Path("/endlessTimestampsMulti")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Multi<OffsetDateTime> endlessTimestampsMulti() {
        return Multi.createFrom().converter(MultiRxConverters.fromObservable(), publisher);
    }
*/

    @GET
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE", "COMPANY_DELETE", "ROLE_COMPANY_CREATE"})
//    @Produces(MediaType.SERVER_SENT_EVENTS)
//    @SseElementType(MediaType.APPLICATION_JSON)
    @Timed(name = "getAllActiveCompaniesTimed",
            description = "Monitor the time getAllActiveCompanies method takes",
            unit = MetricUnits.MILLISECONDS,
            absolute = true)
    @Metered(name = "getAllActiveCompaniesMetered",
            unit = MetricUnits.MILLISECONDS,
            description = "Monitor the rate events occurred",
            absolute = true)
    @Counted(
            name = "getAllActiveCompaniesCounted",
            absolute = true,
            displayName = "getAllActiveCompanies",
            description = "Monitor how many times getAllActiveCompanies method was called")
    @RunOnVirtualThread
    public List<CompanyDto> getAllActiveCompanies(@Context SecurityContext ctx) {
        String name = ctx.getUserPrincipal().getName();
        log.debug("hello {}", name);
        Multi<Company> multi = hasRoleAdmin(ctx) ? Company.findActiveCompanies() : Company
                .findActiveCompaniesByUser(name);
        return multi.onItem().transform(c -> companyMapper.toResource(c))
                .collect().asList().await().indefinitely();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_READ", "ROLE_COMPANY_SAVE"})
    @RunOnVirtualThread
    public Response getById(@PathParam("id") String id, @Context SecurityContext ctx) {
        return getCompanyById(id)
                .onItem().ifNull().failWith(NotFoundException::new)
                .map(c -> {
                    if (hasPermissionToChangeCompany(ctx).test(c)) {
                        return Response.ok(companyMapper.toResource(c)).build();
                    } else {
                        throw new ForbiddenException(String.format("User(%s) does not have access to this resource", ctx.getUserPrincipal().getName()));
                    }
                }).await().indefinitely();
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE"})
    @RunOnVirtualThread
    public Response create(@Valid CompanyDto companyDto, @Context SecurityContext ctx) {
        Company company = companyMapper.toModel(companyDto);
        company.createdByUser = ctx.getUserPrincipal().getName();
        return company.persist()
                .flatMap(i -> company.findById(company.id))
                .map(c -> Response.created(URI.create(String.format("/api/companies/%s", ((Company)c).id)))
                        .entity(companyMapper.toResource(((Company)c)))
                        .build())
                .await().indefinitely();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_CREATE"})
    @RunOnVirtualThread
    public Response update(@Valid CompanyDto companyDto, @PathParam("id") String id, @Context SecurityContext ctx) {
        return getCompanyById(id)
                .onItem().ifNull().failWith(NotFoundException::new)
                .map(c -> {
                    c.name = companyDto.getName();
                    c.lastModifiedByUser = ctx.getUserPrincipal().getName();
                    c.update();
                    return c;
                })
                .map(c -> Response.ok(c).build())
                .await().indefinitely();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_COMPANY_DELETE"})
    @RunOnVirtualThread
    public Response delete(@PathParam("id") String id, @Context SecurityContext ctx) {
        return getCompanyById(id)
                .onItem().ifNull().failWith(NotFoundException::new)
                .map(c -> {
                    if (hasPermissionToChangeCompany(ctx).test(c)) {
                        return c.delete();
                    } else {
                        throw new ForbiddenException(String.format("User(%s) does not have access to delete this resource", ctx.getUserPrincipal().getName()));
                    }
                })
                .map(c -> Response.noContent().build())
                .await().indefinitely();
    }

    private Uni<Company> getCompanyById(String id) {
        return Company.findById(new ObjectId(id));
    }

    private boolean hasRoleAdmin(SecurityContext ctx) {
        return ctx.isUserInRole("ROLE_ADMIN");
    }

    private Predicate<Company> hasPermissionToChangeCompany(SecurityContext ctx) {
        return c -> hasRoleAdmin(ctx) || c.createdByUser.equals(ctx.getUserPrincipal().getName());
    }

    @Mapper(componentModel = "cdi")
    interface CompanyMapper {
        @Mapping(target = "id", expression = "java(company.id.toHexString())")
        CompanyDto toResource(Company company);

        @Mapping(target = "id", expression = "java((companyDto.getId() != null ? new org.bson.types.ObjectId(companyDto.getId()) : null))")
        Company toModel(CompanyDto companyDto);
    }
}