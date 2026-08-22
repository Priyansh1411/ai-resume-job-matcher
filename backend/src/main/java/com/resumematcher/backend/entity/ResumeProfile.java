package com.resumematcher.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "resume_profiles")
public class ResumeProfile {

	@Id
	@Column(name = "id", length = 36, updatable = false)
	private String id;

	@Column(name = "resume_id", nullable = false, length = 36, updatable = false)
	private String resumeId;

	@Column(name = "full_name", length = 255)
	private String fullName;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "phone", length = 50)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "profile_status", nullable = false, length = 30)
	private ProfileStatus profileStatus;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public ResumeProfile() {
	}

	@PrePersist
	protected void prePersist() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
		if (profileStatus == null) {
			profileStatus = ProfileStatus.PENDING;
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getResumeId() {
		return resumeId;
	}

	public void setResumeId(String resumeId) {
		this.resumeId = resumeId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public ProfileStatus getProfileStatus() {
		return profileStatus;
	}

	public void setProfileStatus(ProfileStatus profileStatus) {
		this.profileStatus = profileStatus;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
