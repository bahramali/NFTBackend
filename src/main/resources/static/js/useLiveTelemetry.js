import { useEffect, useMemo, useRef, useState } from 'react';

let sharedClient = null;
let connectingPromise = null;
let activeHookCount = 0;

const CONNECTION_HEADERS = {};
const SOCKET_ENDPOINT = '/ws';
const DEFAULT_METRICS = ['telemetry'];

function ensureClient() {
  if (sharedClient && sharedClient.connected) {
    return Promise.resolve(sharedClient);
  }

  if (connectingPromise) {
    return connectingPromise;
  }

  if (typeof window === 'undefined' || !window.SockJS || !window.Stomp) {
    return Promise.reject(new Error('SockJS/Stomp not available on window'));
  }

  const socket = new window.SockJS(SOCKET_ENDPOINT);
  const client = window.Stomp.over(socket);
  client.reconnect_delay = 0;

  connectingPromise = new Promise((resolve, reject) => {
    client.connect(
      CONNECTION_HEADERS,
      () => {
        sharedClient = client;
        connectingPromise = null;
        resolve(client);
      },
      (error) => {
        connectingPromise = null;
        reject(error);
      },
    );
  });

  return connectingPromise;
}

function disconnectClient() {
  if (sharedClient && sharedClient.connected) {
    sharedClient.disconnect();
  }
  sharedClient = null;
  connectingPromise = null;
}

function normalizeTopics({ rackId, selectedNodes, metrics }) {
  if (!rackId || selectedNodes.length === 0) {
    return [];
  }

  const uniqueTopics = new Set();

  selectedNodes.forEach((node) => {
    const nodeTopic = typeof node === 'string' ? node : node.topic;
    const nodeId = typeof node === 'string' ? node : node.id;

    if (nodeTopic) {
      const normalized = nodeTopic.startsWith('/topic/')
        ? nodeTopic
        : `/topic/${nodeTopic}`;
      uniqueTopics.add(normalized);
      return;
    }

    metrics.forEach((metric) => {
      const normalizedMetric = metric || 'telemetry';
      uniqueTopics.add(`/topic/hydroleaf/v1/${rackId}/${nodeId}/${normalizedMetric}`);
    });
  });

  return Array.from(uniqueTopics);
}

function parsePayload(message) {
  try {
    return JSON.parse(message.body);
  } catch (error) {
    return message.body;
  }
}

export function useLiveTelemetry({ rackId, selectedNodes = [], metrics = DEFAULT_METRICS }) {
  const [latest, setLatest] = useState({});
  const subscriptionsRef = useRef(new Map());
  const lastRackRef = useRef(rackId);

  const topics = useMemo(
    () => normalizeTopics({ rackId, selectedNodes, metrics }),
    [rackId, selectedNodes, metrics],
  );

  useEffect(() => {
    activeHookCount += 1;
    return () => {
      activeHookCount -= 1;
      if (activeHookCount <= 0) {
        disconnectClient();
      }
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    const unsubscribeAll = () => {
      subscriptionsRef.current.forEach((subscription) => {
        subscription.unsubscribe();
      });
      subscriptionsRef.current.clear();
    };

    if (lastRackRef.current !== rackId) {
      unsubscribeAll();
      lastRackRef.current = rackId;
    }

    if (topics.length === 0) {
      unsubscribeAll();
      return () => {
        unsubscribeAll();
      };
    }

    ensureClient()
      .then((client) => {
        if (cancelled) {
          return;
        }

        topics.forEach((topic) => {
          if (subscriptionsRef.current.has(topic)) {
            return;
          }
          const subscription = client.subscribe(topic, (message) => {
            const payload = parsePayload(message);
            setLatest((prev) => ({
              ...prev,
              [topic]: payload,
            }));
          });
          subscriptionsRef.current.set(topic, subscription);
        });

        subscriptionsRef.current.forEach((subscription, topic) => {
          if (!topics.includes(topic)) {
            subscription.unsubscribe();
            subscriptionsRef.current.delete(topic);
          }
        });
      })
      .catch(() => {
        if (!cancelled) {
          unsubscribeAll();
        }
      });

    return () => {
      cancelled = true;
      unsubscribeAll();
    };
  }, [rackId, topics]);

  return { latest, topics };
}
