package com.microservice.quarkus.mapper;

import com.microservice.quarkus.dto.CompanyDto;
import com.microservice.quarkus.model.Company;
import com.microservice.quarkus.model.CompanyHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@Mapper(componentModel = "jakarta")
public interface CompanyMapper {
    @Mapping(target = "id", expression = "java(company.id.toHexString())")
    CompanyDto toResource(Company company);

    @Mapping(target = "id", expression = "java((companyDto.getId() != null ? new org.bson.types.ObjectId(companyDto.getId()) : null))")
    Company toModel(CompanyDto companyDto);

    List<CompanyDto> toResource(List<Company> companies);

    default Page<CompanyDto> toResource(CompanyHelper companyHelper, io.quarkus.panache.common.Page page) {
        return PageableExecutionUtils.getPage(toResource(companyHelper.companies()), PageRequest.of(page.index, page.size), companyHelper::count);
    }
}
