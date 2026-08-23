import { extractErrorMessage } from './apiErrors'
import { protectedFetch, UnauthorizedError } from './apiClient'

export type ResumeAnalysisResult = {
  strengths: string[]
  gaps: string[]
  suggestions: string[]
}

export class ResumeAnalysisError extends Error {}

function isResumeAnalysisResult(value: unknown): value is ResumeAnalysisResult {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Record<string, unknown>
  return (
    Array.isArray(candidate.strengths) &&
    Array.isArray(candidate.gaps) &&
    Array.isArray(candidate.suggestions)
  )
}

export async function fetchResumeAnalysis(
  resumeId: string,
  jobDescription: string,
): Promise<ResumeAnalysisResult> {
  let response: Response
  let data: unknown
  try {
    ;({ response, data } = await protectedFetch(`/api/resumes/${encodeURIComponent(resumeId)}/analysis`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jobDescription }),
    }))
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      throw err
    }
    throw new ResumeAnalysisError('Could not reach the server. Please check your connection and try again.')
  }

  if (!response.ok) {
    throw new ResumeAnalysisError(extractErrorMessage(data) ?? 'Could not generate the AI analysis.')
  }

  if (!isResumeAnalysisResult(data)) {
    throw new ResumeAnalysisError('Received an unexpected response from the server.')
  }

  return data
}
