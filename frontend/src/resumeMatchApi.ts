import { extractErrorMessage } from './apiErrors'

export type ResumeMatchResult = {
  matchScorePercentage: number
  matchedSkills: string[]
  missingSkills: string[]
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
    Array.isArray(candidate.missingSkills)
  )
}

export async function fetchResumeMatch(
  resumeId: string,
  jobDescription: string,
): Promise<ResumeMatchResult> {
  let response: Response
  try {
    response = await fetch(`/api/resumes/${encodeURIComponent(resumeId)}/match`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jobDescription }),
    })
  } catch {
    throw new ResumeMatchError('Could not reach the server. Please check your connection and try again.')
  }

  let data: unknown = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  if (!response.ok) {
    throw new ResumeMatchError(extractErrorMessage(data) ?? 'Could not calculate the match score.')
  }

  if (!isResumeMatchResult(data)) {
    throw new ResumeMatchError('Received an unexpected response from the server.')
  }

  return data
}
