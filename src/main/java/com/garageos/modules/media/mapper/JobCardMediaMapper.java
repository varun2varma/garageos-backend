package com.garageos.modules.media.mapper;

import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobCardMediaMapper {

    JobCardMediaResponse toResponse(JobCardMedia media);

    List<JobCardMediaResponse> toResponseList(List<JobCardMedia> media);
}
