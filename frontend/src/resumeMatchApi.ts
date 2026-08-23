import { extractErrorMessage } from './apiErrors'
import { protectedFetch, UnauthorizedError } from './apiClient'

export type ResumeMatchResult = {
  matchScorePercentage: number
  matchedSkills: string[]
  missingSkills: string[]
  matchedRequiredSkills: string[]
  missingRequiredSkills: string[]
  matchedPreferredSkills: string[]
  missingPreferredSkills: string[]
}

export class ResumeMatchError extends Error {}

function isResumeMatchResult(value: unknown): value is ResumeMatchResult {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.matchScorePercentage === 'number' &&
    Array.isArray(candidate.matchedSkills) &&
    Array.isArray(candidate.missingSkills) &&
    Array.isArray(candidate.matchedRequiredSkills) &&
    Array.isArray(candidate.missingRequiredSkills) &&
    Array.isArray(candidate.matchedPreferredSkills) &&
    Array.isArray(candidate.missingPreferredSkills)
  )
}

export async function fetchResumeMatch(
  resumeId: string,
  jobDescription: string,
): Promise<ResumeMatchResult> {
  let response: Response
  let data: unknown
  try {
    ;({ response, data } = await protectedFetch(`/api/resumes/${encodeURIComponent(resumeId)}/match`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jobDescription }),
    }))
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      throw err
    }
    throw new ResumeMatchError('Could not reach the server. Please check your connection and try again.')
  }

  if (!response.ok) {
    throw new ResumeMatchError(extractErrorMessage(data) ?? 'Could not calculate the match score.')
  }

  if (!isResumeMatchResult(data)) {
    throw new ResumeMatchError('Received an unexpected response from the server.')
  }

  return data
}
