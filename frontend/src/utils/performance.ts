/**
 * Performance optimization utilities
 * Provides debounce, throttle, and lazy loading helpers
 */

/**
 * Debounce function - delays execution until after wait time has elapsed
 * since the last call. Useful for input handlers, resize events, etc.
 *
 * @param func - The function to debounce
 * @param wait - Wait time in milliseconds
 * @returns Debounced function
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    if (timeoutId !== null) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      func.apply(context, args)
      timeoutId = null
    }, wait)
  }
}

/**
 * Throttle function - ensures function is called at most once per specified time period
 * Useful for scroll handlers, mousemove events, etc.
 *
 * @param func - The function to throttle
 * @param limit - Time limit in milliseconds
 * @returns Throttled function
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean = false
  let lastResult: ReturnType<T>

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    if (!inThrottle) {
      lastResult = func.apply(context, args)
      inThrottle = true

      setTimeout(() => {
        inThrottle = false
      }, limit)
    }

    return lastResult
  }
}

/**
 * Request animation frame throttle - ensures function is called at most once per frame
 * Best for animation and visual updates
 *
 * @param func - The function to throttle
 * @returns RAF-throttled function
 */
export function rafThrottle<T extends (...args: any[]) => any>(
  func: T
): (...args: Parameters<T>) => void {
  let rafId: number | null = null
  let lastArgs: Parameters<T> | null = null

  return function (this: any, ...args: Parameters<T>) {
    const context = this
    lastArgs = args

    if (rafId === null) {
      rafId = requestAnimationFrame(() => {
        if (lastArgs !== null) {
          func.apply(context, lastArgs)
        }
        rafId = null
        lastArgs = null
      })
    }
  }
}

/**
 * Memoization helper for expensive computations
 *
 * @param func - The function to memoize
 * @returns Memoized function
 */
export function memoize<T extends (...args: any[]) => any>(
  func: T
): T {
  const cache = new Map<string, ReturnType<T>>()

  return function (this: any, ...args: Parameters<T>): ReturnType<T> {
    const key = JSON.stringify(args)

    if (cache.has(key)) {
      return cache.get(key)!
    }

    const result = func.apply(this, args)
    cache.set(key, result)
    return result
  } as T
}

/**
 * Lazy image loader with Intersection Observer
 *
 * @param selector - CSS selector for lazy images
 */
export function lazyLoadImages(selector: string = '[data-lazy]') {
  if (!('IntersectionObserver' in window)) {
    // Fallback: load all images immediately
    const images = document.querySelectorAll(selector)
    images.forEach((img: any) => {
      if (img.dataset.lazy) {
        img.src = img.dataset.lazy
      }
    })
    return
  }

  const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const img = entry.target as HTMLImageElement
        if (img.dataset.lazy) {
          img.src = img.dataset.lazy
          img.removeAttribute('data-lazy')
          observer.unobserve(img)
        }
      }
    })
  })

  const images = document.querySelectorAll(selector)
  images.forEach((img) => imageObserver.observe(img))
}

/**
 * Batch DOM updates to minimize reflows
 *
 * @param updates - Array of update functions
 */
export function batchDOMUpdates(updates: Array<() => void>) {
  requestAnimationFrame(() => {
    updates.forEach((update) => update())
  })
}

/**
 * Virtual scroll helper for large lists
 * Calculates visible items based on scroll position
 *
 * @param itemCount - Total number of items
 * @param itemHeight - Height of each item in pixels
 * @param scrollTop - Current scroll position
 * @param containerHeight - Height of the container
 * @param overscan - Number of extra items to render (buffer)
 * @returns Object with start index, end index, and offset
 */
export function calculateVisibleRange(
  itemCount: number,
  itemHeight: number,
  scrollTop: number,
  containerHeight: number,
  overscan: number = 3
) {
  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - overscan)
  const endIndex = Math.min(
    itemCount - 1,
    Math.ceil((scrollTop + containerHeight) / itemHeight) + overscan
  )
  const offsetY = startIndex * itemHeight

  return {
    startIndex,
    endIndex,
    offsetY,
    visibleCount: endIndex - startIndex + 1
  }
}
