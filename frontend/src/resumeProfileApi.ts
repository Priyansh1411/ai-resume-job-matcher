import { extractErrorMessage } from './apiErrors'

export type ResumeProfileResult = {
  fullName: string | null
  email: string | null
  phone: string | null
  skills: string[]
  profileStatus: string
}

export class ResumeProfileError extends Error {}

function isResumeProfileResult(value: unknown): value is ResumeProfileResult {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Record<string, unknown>
  return (
    (candidate.fullName === null || typeof candidate.fullName === 'string') &&
    (candidate.email === null || typeof candidate.email === 'string') &&
    (candidate.phone === null || typeof candidate.phone === 'string') &&
    Array.isArray(candidate.skills) &&
    typeof candidate.profileStatus === 'string'
  )
}

export async function fetchResumeProfile(resumeId: string): Promise<ResumeProfileResult> {
  let response: Response
  try {
    response = await fetch(`/api/resumes/${encodeURIComponent(resumeId)}/profile`)
  } catch {
    throw new ResumeProfileError('Could not reach the server. Please check your connection and try again.')
  }

  let data: unknown = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  if (!response.ok) {
    throw new ResumeProfileError(extractErrorMessage(data) ?? 'Could not load the resume profile.')
  }

  if (!isResumeProfileResult(data)) {
    throw new ResumeProfileError('Received an unexpected response from the server.')
  }

  return data
}
