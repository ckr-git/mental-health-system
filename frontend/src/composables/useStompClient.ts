import { ref, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client, over } from 'stompjs'
import type { Frame, Message } from 'stompjs'
import { useUserStore } from '@/stores/user'

interface StompOptions {
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: string) => void
}

export function useStompClient(options: StompOptions = {}) {
  const connected = ref(false)
  const userStore = useUserStore()
  let stompClient: Client | null = null
  let subscriptions: { id: string; unsubscribe: () => void }[] = []

  const connect = () => {
    if (connected.value || !userStore.token) return

    const socket = new SockJS('/ws')
    stompClient = over(socket)

    // Disable STOMP debug logging in production
    stompClient.debug = (() => {}) as any

    // Use the overload that accepts (login, passcode, connectCb, errorCb, host)
    // We pass empty strings for login/passcode since we use JWT via native headers
    const connectHeaders: Record<string, string> = {
      Authorization: `Bearer ${userStore.token}`
    }

    ;(stompClient as any).connect(
      connectHeaders,
      () => {
        connected.value = true
        options.onConnect?.()
      },
      (error: any) => {
        connected.value = false
        const errorMsg = typeof error === 'string' ? error : error.toString()
        console.error('STOMP connection error:', errorMsg)
        options.onError?.(errorMsg)

        // Auto-reconnect after 5 seconds
        setTimeout(() => {
          if (!connected.value && userStore.token) {
            connect()
          }
        }, 5000)
      }
    )
  }

  const disconnect = () => {
    if (stompClient && connected.value) {
      subscriptions.forEach(sub => sub.unsubscribe())
      subscriptions = []
      stompClient.disconnect(() => {
        connected.value = false
        options.onDisconnect?.()
      })
    }
  }

  const subscribe = (destination: string, callback: (message: Message) => void) => {
    if (!stompClient || !connected.value) {
      console.warn('STOMP not connected, cannot subscribe to', destination)
      return null
    }

    const sub = stompClient.subscribe(destination, callback)
    subscriptions.push(sub)
    return sub
  }

  const send = (destination: string, body: Record<string, unknown>, headers: Record<string, string> = {}) => {
    if (!stompClient || !connected.value) {
      console.warn('STOMP not connected, cannot send to', destination)
      return
    }
    stompClient.send(destination, headers, JSON.stringify(body))
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    connect,
    disconnect,
    subscribe,
    send
  }
}
