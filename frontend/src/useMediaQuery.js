import { useEffect, useState } from 'react'

/** true enquanto a consulta de mídia (ex.: largura de celular) for verdadeira. */
export default function useMediaQuery(query) {
  const [match, setMatch] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const list = window.matchMedia(query)
    const update = () => setMatch(list.matches)
    list.addEventListener('change', update)
    return () => list.removeEventListener('change', update)
  }, [query])
  return match
}
