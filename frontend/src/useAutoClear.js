import { useEffect } from 'react'

/** Limpa a mensagem depois de alguns segundos (sucesso some antes de erro). */
export default function useAutoClear(message, clear, ms) {
  useEffect(() => {
    if (!message) return
    const timer = setTimeout(() => clear(''), ms)
    return () => clearTimeout(timer)
  }, [message, clear, ms])
}
