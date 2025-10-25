package se.hydroleaf.model;

public enum TopicName {
    growSensors,
    waterTank,
    actuatorOxygenPump,
    germinationTopic;

    public static TopicName fromMqttTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }

        String prefix = topic;
        int slash = topic.indexOf('/');
        if (slash >= 0) {
            prefix = topic.substring(0, slash);
        }

        for (TopicName value : values()) {
            if (value.name().equalsIgnoreCase(prefix)) {
                return value;
            }
        }

        return null;
    }
}
