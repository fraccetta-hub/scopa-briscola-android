'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Input } from '@/ui/Input'
import { Button } from '@/ui/Button'

interface SearchBoxProps {
  onSearch: (query: string) => void
  initialValue?: string
}

export function SearchBox({ onSearch, initialValue = '' }: SearchBoxProps) {
  const t = useTranslations('search')
  const [value, setValue] = useState(initialValue)

  function handleSubmit() {
    onSearch(value)
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      onSearch(value)
    }
  }

  return (
    <div className="flex gap-2 w-full max-w-2xl">
      <div className="flex-1">
        <Input
          variant="search"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t('placeholder')}
          aria-label={t('placeholder')}
        />
      </div>
      <Button onClick={handleSubmit} type="button">
        {t('button')}
      </Button>
    </div>
  )
}
