package com.garageos.modules.complaint.service;

import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintService {

    ComplaintResponse createComplaint(
            Long jobCardId,
            CreateComplaintRequest request);

    List<ComplaintResponse> createComplaintList(
            Long jobCardId,
            List<CreateComplaintRequest> request);
    ComplaintResponse getComplaint(Long id);

    List<ComplaintResponse> getComplaints(Long jobCardId);

    ComplaintResponse updateComplaint(
            Long id,
            CreateComplaintRequest request);

    void deleteComplaint(Long id);
}