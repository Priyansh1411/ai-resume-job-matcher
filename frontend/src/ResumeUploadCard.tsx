import { useRef, useState } from 'react'
import type { ChangeEvent, DragEvent } from 'react'
import { formatFileSizeInMb } from './formatFileSize'

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024

const ALLOWED_TYPES = new Set([
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
])

function hasAllowedExtension(fileName: string): boolean {
  const lowerName = fileName.toLowerCase()
  return lowerName.endsWith('.pdf') || lowerName.endsWith('.docx')
}

function isAllowedFileType(file: File): boolean {
  if (ALLOWED_TYPES.has(file.type)) {
    return true
  }
  return file.type === '' && hasAllowedExtension(file.name)
}

function validateFile(file: File): string | null {
  if (file.size === 0) {
    return 'This file is empty. Please choose a different file.'
  }
  if (!isAllowedFileType(file)) {
    return 'Unsupported file type. Please upload a PDF or DOCX file.'
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'This file is too large. Maximum allowed size is 10 MB.'
  }
  return null
}

type ResumeUploadCardProps = {
  onFileSelected: (file: File | null) => void
}

function ResumeUploadCard({ onFileSelected }: ResumeUploadCardProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isDragActive, setIsDragActive] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function acceptFile(file: File) {
    const validationError = validateFile(file)
    if (validationError) {
      setError(validationError)
      setSelectedFile(null)
      onFileSelected(null)
      return
    }
    setError(null)
    setSelectedFile(file)
    onFileSelected(file)
  }

  function handleChooseClick() {
    fileInputRef.current?.click()
  }

  function handleInputChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (file) {
      acceptFile(file)
    }
    event.target.value = ''
  }

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragActive(true)
  }

  function handleDragLeave(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragActive(false)
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragActive(false)
    const file = event.dataTransfer.files?.[0]
    if (file) {
      acceptFile(file)
    }
  }

  function handleRemoveFile() {
    setSelectedFile(null)
    setError(null)
    onFileSelected(null)
  }

  return (
    <div className="card">
      <h2>Upload Your Resume</h2>
      <p className="card__subtitle">PDF or DOCX, maximum 10 MB</p>

      <div
        className={`dropzone${isDragActive ? ' dropzone--active' : ''}`}
        role="group"
        aria-label="Resume file drop area"
        onDragOver={handleDragOver}
        onDragEnter={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {selectedFile ? (
          <>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M14 3v4a1 1 0 0 0 1 1h4" />
              <path d="M17 21H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7l5 5v11a2 2 0 0 1-2 2Z" />
              <path d="m9 14 2 2 4-4" />
            </svg>
            <p className="dropzone__text selected-file__name">{selectedFile.name}</p>
            <p className="dropzone__hint">{formatFileSizeInMb(selectedFile.size)}</p>
            <button type="button" className="btn btn--secondary" onClick={handleRemoveFile}>
              Remove file
            </button>
          </>
        ) : (
          <>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M12 16V4" />
              <path d="M7 9l5-5 5 5" />
              <path d="M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" />
            </svg>
            <p className="dropzone__text">Drag and drop your resume here</p>
            <p className="dropzone__hint">or</p>
            <button type="button" className="btn btn--secondary" onClick={handleChooseClick}>
              Choose Resume
            </button>
          </>
        )}
      </div>

      <label className="visually-hidden" htmlFor="resume-file-input">
        Resume file (PDF or DOCX, maximum 10 MB)
      </label>
      <input
        ref={fileInputRef}
        id="resume-file-input"
        type="file"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        className="visually-hidden"
        onChange={handleInputChange}
      />

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export default ResumeUploadCard
