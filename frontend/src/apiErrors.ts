export function extractErrorMessage(value: unknown): string | null {
  if (value && typeof value === 'object' && typeof (value as { error?: unknown }).error === 'string') {
    return (value as { error: string }).error
  }
  return null
}
