import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import '@testing-library/jest-dom'
import { SearchBox } from '../SearchBox'

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}))

describe('SearchBox', () => {
  it('renders input and calls onSearch on submit', async () => {
    const onSearch = vi.fn()
    render(<SearchBox onSearch={onSearch} />)
    const input = screen.getByRole('textbox')
    await userEvent.type(input, 'zapatería')
    await userEvent.keyboard('{Enter}')
    expect(onSearch).toHaveBeenCalledWith('zapatería')
  })

  it('calls onSearch on button click', async () => {
    const onSearch = vi.fn()
    render(<SearchBox onSearch={onSearch} />)
    await userEvent.type(screen.getByRole('textbox'), 'farmacia')
    await userEvent.click(screen.getByRole('button'))
    expect(onSearch).toHaveBeenCalledWith('farmacia')
  })
})
