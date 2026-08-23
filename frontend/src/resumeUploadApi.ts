import { extractErrorMessage } from './apiErrors'
import { protectedFetch, UnauthorizedError } from './apiClient'

export type ResumeUploadResult = {
  id: string
  originalFilename: string
  contentType: string
  fileSizeBytes: number
  processingStatus: string
}

export class ResumeUploadError extends Error {}

function isResumeUploadResult(value: unknown): value is ResumeUploadResult {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.originalFilename === 'string' &&
    typeof candidate.contentType === 'string' &&
    typeof candidate.fileSizeBytes === 'number' &&
    typeof candidate.processingStatus === 'string'
  )
}

export async function uploadResume(file: File): Promise<ResumeUploadResult> {
  const formData = new FormData()
  formData.append('file', file)

  let response: Response
  let data: unknown
  try {
    ;({ response, data } = await protectedFetch('/api/resumes/upload', {
      method: 'POST',
      body: formData,
    }))
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      throw err
    }
    throw new ResumeUploadError('Could not reach the server. Please check your connection and try again.')
  }

  if (!response.ok) {
    throw new ResumeUploadError(extractErrorMessage(data) ?? 'Upload failed. Please try again.')
  }

  if (!isResumeUploadResult(data)) {
    throw new ResumeUploadError('Received an unexpected response from the server.')
  }

  return data
}
