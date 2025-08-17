package se.hydroleaf.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.mqtt.TopicPublisher;
import se.hydroleaf.service.StatusService;

import java.time.Duration;
import java.time.Instant;

/**
 * Periodic tasks related to live device feed.
 */
@Slf4j
@Service
public class LiveFeedScheduler {

    private final StatusService statusService;
    private final TopicPublisher topicPublisher;
    private final LastSeenRegistry lastSeen;
    private final ObjectMapper objectMapper;

    public LiveFeedScheduler(StatusService statusService,
                             TopicPublisher topicPublisher,
                             LastSeenRegistry lastSeen,
                             ObjectMapper objectMapper) {
        this.statusService = statusService;
        this.topicPublisher = topicPublisher;
        this.lastSeen = lastSeen;
        this.objectMapper = objectMapper;
    }
final String msg = """
        {
          "systems": {
            "S02": {
              "lastUpdate": "2025-08-16T07:10:34.380345500Z",
              "actuators": {
                "airPump": {
                  "average": null,
                  "unit": "status",
                  "deviceCount": 0
                }
              },
              "water": {
                "dissolvedTemp": {
                  "average": null,
                  "unit": "°C",
                  "deviceCount": 0
                },
                "dissolvedOxygen": {
                  "average": null,
                  "unit": "mg/L",
                  "deviceCount": 0
                },
                "pH": {
                  "average": null,
                  "unit": "pH",
                  "deviceCount": 0
                },
                "dissolvedEC": {
                  "average": null,
                  "unit": "mS/cm",
                  "deviceCount": 0
                },
                "dissolvedTDS": {
                  "average": null,
                  "unit": "ppm",
                  "deviceCount": 0
                }
              },
              "environment": {
                "light": {
                  "average": 121.2,
                  "unit": "lux",
                  "deviceCount": 2
                },
                "humidity": {
                  "average": null,
                  "unit": "%",
                  "deviceCount": 0
                },
                "temperature": {
                  "average": null,
                  "unit": "°C",
                  "deviceCount": 0
                }
              },
              "layers": [
                {
                  "layerId": "L01",
                  "lastUpdate": "2025-08-16T07:10:34.380345500Z",
                  "actuators": {
                    "airPump": {
                      "average": null,
                      "unit": "status",
                      "deviceCount": 0
                    }
                  },
                  "water": {
                    "dissolvedTemp": {
                      "average": 0.0,
                      "unit": "°C",
                      "deviceCount": 0
                    },
                    "dissolvedOxygen": {
                      "average": 0.0,
                      "unit": "mg/L",
                      "deviceCount": 0
                    },
                    "pH": {
                      "average": 0.0,
                      "unit": "pH",
                      "deviceCount": 0
                    },
                    "dissolvedEC": {
                      "average": 0.0,
                      "unit": "mS/cm",
                      "deviceCount": 0
                    },
                    "dissolvedTDS": {
                      "average": 0.0,
                      "unit": "ppm",
                      "deviceCount": 0
                    }
                  },
                  "environment": {
                    "light": {
                      "average": 210.1,
                      "unit": "lux",
                      "deviceCount": 1
                    },
                    "humidity": {
                      "average": 0.0,
                      "unit": "%",
                      "deviceCount": 0
                    },
                    "temperature": {
                      "average": 0.0,
                      "unit": "°C",
                      "deviceCount": 0
                    }
                  }
                },
                {
                  "layerId": "L02",
                  "lastUpdate": "2025-08-16T07:10:34.373086700Z",
                  "actuators": {
                    "airPump": {
                      "average": null,
                      "unit": "status",
                      "deviceCount": 0
                    }
                  },
                  "water": {
                    "dissolvedTemp": {
                      "average": 0.0,
                      "unit": "°C",
                      "deviceCount": 0
                    },
                    "dissolvedOxygen": {
                      "average": 0.0,
                      "unit": "mg/L",
                      "deviceCount": 0
                    },
                    "pH": {
                      "average": 0.0,
                      "unit": "pH",
                      "deviceCount": 0
                    },
                    "dissolvedEC": {
                      "average": 0.0,
                      "unit": "mS/cm",
                      "deviceCount": 0
                    },
                    "dissolvedTDS": {
                      "average": 0.0,
                      "unit": "ppm",
                      "deviceCount": 0
                    }
                  },
                  "environment": {
                    "light": {
                      "average": 32.3,
                      "unit": "lux",
                      "deviceCount": 1
                    },
                    "humidity": {
                      "average": 0.0,
                      "unit": "%",
                      "deviceCount": 0
                    },
                    "temperature": {
                      "average": 0.0,
                      "unit": "°C",
                      "deviceCount": 0
                    }
                  }
                }
              ]
            },
            "S01": {
              "lastUpdate": "2025-08-16T07:10:34.366519600Z",
              "actuators": {
                "airPump": {
                  "average": 0.0,
                  "unit": "status",
                  "deviceCount": 1
                }
              },
              "water": {
                "dissolvedTemp": {
                  "average": 22.4,
                  "unit": "°C",
                  "deviceCount": 1
                },
                "dissolvedOxygen": {
                  "average": 4.6,
                  "unit": "mg/L",
                  "deviceCount": 1
                },
                "pH": {
                  "average": null,
                  "unit": "pH",
                  "deviceCount": 0
                },
                "dissolvedEC": {
                  "average": 1.6,
                  "unit": "mS/cm",
                  "deviceCount": 1
                },
                "dissolvedTDS": {
                  "average": 1006.4,
                  "unit": "ppm",
                  "deviceCount": 1
                }
              },
              "environment": {
                "light": {
                  "average": 5515.8,
                  "unit": "lux",
                  "deviceCount": 3
                },
                "humidity": {
                  "average": 36.0,
                  "unit": "%",
                  "deviceCount": 3
                },
                "temperature": {
                  "average": 26.1,
                  "unit": "°C",
                  "deviceCount": 3
                }
              },
              "layers": [
                {
                  "layerId": "L01",
                  "lastUpdate": "2025-08-16T07:10:34.366519600Z",
                  "actuators": {
                    "airPump": {
                      "average": 0.0,
                      "unit": "status",
                      "deviceCount": 1
                    }
                  },
                  "water": {
                    "dissolvedTemp": {
                      "average": 22.4,
                      "unit": "°C",
                      "deviceCount": 1
                    },
                    "dissolvedOxygen": {
                      "average": 4.6,
                      "unit": "mg/L",
                      "deviceCount": 1
                    },
                    "pH": {
                      "average": 0.0,
                      "unit": "pH",
                      "deviceCount": 0
                    },
                    "dissolvedEC": {
                      "average": 1.6,
                      "unit": "mS/cm",
                      "deviceCount": 1
                    },
                    "dissolvedTDS": {
                      "average": 1006.4,
                      "unit": "ppm",
                      "deviceCount": 1
                    }
                  },
                  "environment": {
                    "light": {
                      "average": 179.0,
                      "unit": "lux",
                      "deviceCount": 2
                    },
                    "humidity": {
                      "average": 38.2,
                      "unit": "%",
                      "deviceCount": 2
                    },
                    "temperature": {
                      "average": 24.9,
                      "unit": "°C",
                      "deviceCount": 2
                    }
                  }
                },
                {
                  "layerId": "L02",
                  "lastUpdate": "2025-08-16T07:10:34.358981Z",
                  "actuators": {
                    "airPump": {
                      "average": null,
                      "unit": "status",
                      "deviceCount": 0
                    }
                  },
                  "water": {
                    "dissolvedTemp": {
                      "average": 0.0,
                      "unit": "°C",
                      "deviceCount": 0
                    },
                    "dissolvedOxygen": {
                      "average": 0.0,
                      "unit": "mg/L",
                      "deviceCount": 0
                    },
                    "pH": {
                      "average": 0.0,
                      "unit": "pH",
                      "deviceCount": 0
                    },
                    "dissolvedEC": {
                      "average": 0.0,
                      "unit": "mS/cm",
                      "deviceCount": 0
                    },
                    "dissolvedTDS": {
                      "average": 0.0,
                      "unit": "ppm",
                      "deviceCount": 0
                    }
                  },
                  "environment": {
                    "light": {
                      "average": 16189.3,
                      "unit": "lux",
                      "deviceCount": 1
                    },
                    "humidity": {
                      "average": 31.7,
                      "unit": "%",
                      "deviceCount": 1
                    },
                    "temperature": {
                      "average": 28.6,
                      "unit": "°C",
                      "deviceCount": 1
                    }
                  }
                }
              ]
            }
          }
        }
        
        """;
    @Scheduled(fixedRateString = "${livefeed.rate:2000}")
    public void sendLiveNow() {
        log.info("sendLiveNow invoked");
        try {
//            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
//            String payload = objectMapper.writeValueAsString(snapshot);
            topicPublisher.publish("/topic/live_now", msg);
            log.info("payload: ");
//        } catch (JsonProcessingException e) {
//            log.warn("Failed to serialize LiveNowSnapshot", e);
        } catch (Exception e) {
            log.warn("sendLiveNow failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }
}
