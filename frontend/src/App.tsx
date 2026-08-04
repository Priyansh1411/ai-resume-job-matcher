import { useState } from 'react'
import ResumeUploadCard from './ResumeUploadCard'
import JobDescriptionCard, { MIN_JOB_DESCRIPTION_CHARACTERS } from './JobDescriptionCard'
import UploadStatusPanel from './UploadStatusPanel'
import { ResumeUploadError, uploadResume } from './resumeUploadApi'
import type { ResumeUploadResult } from './resumeUploadApi'
import './App.css'

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error'

function App() {
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState('')
  const [uploadStatus, setUploadStatus] = useState<UploadStatus>('idle')
  const [uploadResult, setUploadResult] = useState<ResumeUploadResult | null>(null)
  const [uploadErrorMessage, setUploadErrorMessage] = useState<string | null>(null)

  const isResumeValid = resumeFile !== null
  const isJobDescriptionValid =
    jobDescription.trim().length >= MIN_JOB_DESCRIPTION_CHARACTERS
  const isAnalyzeEnabled = isResumeValid && isJobDescriptionValid

  function handleResumeFileSelected(file: File | null) {
    setResumeFile(file)
    setUploadStatus('idle')
    setUploadResult(null)
    setUploadErrorMessage(null)
  }

  async function handleAnalyzeClick() {
    if (!isAnalyzeEnabled || uploadStatus === 'uploading' || !resumeFile) {
      return
    }

    setUploadStatus('uploading')
    setUploadErrorMessage(null)

    try {
      const result = await uploadResume(resumeFile)
      setUploadResult(result)
      setUploadStatus('success')
    } catch (err) {
      const message =
        err instanceof ResumeUploadError ? err.message : 'Upload failed. Please try again.'
      setUploadErrorMessage(message)
      setUploadStatus('error')
    }
  }

  return (
    <div className="page">
      <header className="navbar">
        <div className="navbar__inner">
          <a className="brand" href="#top">
            <span className="brand__name">ResumeMatch AI</span>
            <span className="badge">AI Powered</span>
          </a>
          <nav className="nav-links" aria-label="Main navigation">
            <a href="#top">Home</a>
            <a href="#how-it-works">How It Works</a>
          </nav>
          <a className="btn btn--primary" href="#upload">
            Get Started
          </a>
        </div>
      </header>

      <main>
        <section className="hero" id="top">
          <h1>Match Your Resume to the Right Job</h1>
          <p className="hero__description">
            Upload your resume and paste a job description to instantly see
            how well they align, so you can tailor your application before
            you apply.
          </p>
          <div className="feature-pills">
            <span className="pill">Skill Match Analysis</span>
            <span className="pill">Missing Keyword Detection</span>
          </div>
        </section>

        <section
          className="workspace"
          id="upload"
          aria-label="Resume and job description input"
        >
          <ResumeUploadCard onFileSelected={handleResumeFileSelected} />

          <JobDescriptionCard
            value={jobDescription}
            onChange={setJobDescription}
            isAnalyzeEnabled={isAnalyzeEnabled}
            isUploading={uploadStatus === 'uploading'}
            onAnalyzeClick={handleAnalyzeClick}
          />
        </section>

        {(uploadStatus === 'success' || uploadStatus === 'error') && (
          <div className="status-panel-wrapper">
            <UploadStatusPanel
              status={uploadStatus}
              result={uploadResult}
              errorMessage={uploadErrorMessage}
            />
          </div>
        )}

        <section
          className="how-it-works"
          id="how-it-works"
          aria-label="How it works"
        >
          <h2 className="section-title">How It Works</h2>
          <ol className="steps">
            <li className="step">
              <span className="step__number" aria-hidden="true">1</span>
              <h3>Upload Resume</h3>
              <p>Add your resume in PDF or DOCX format.</p>
            </li>
            <li className="step">
              <span className="step__number" aria-hidden="true">2</span>
              <h3>Add Job Description</h3>
              <p>Paste the job posting you are applying for.</p>
            </li>
            <li className="step">
              <span className="step__number" aria-hidden="true">3</span>
              <h3>View Match Results</h3>
              <p>See your compatibility score and suggestions.</p>
            </li>
          </ol>
        </section>
      </main>
    </div>
  )
}

export default App
