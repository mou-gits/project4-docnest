package ca.docnest.server;

import ca.docnest.server.handlers.Handlers;
import ca.docnest.shared.protocol.*;

public class PacketRouter {

    public static void route(
            DataPacket packet,
            ClientSession session,
            PacketTransport transport
    ) throws Exception {

        PacketType type = packet.getCommand();

        switch (type) {

            case LOGIN:
                Handlers.handleLogin(packet, session, transport);
                break;

            case LIST_FILES:
                Handlers.handleListFiles(packet, session, transport);
                break;

            case UPLOAD_INIT:
                Handlers.handleUploadInit(packet, session, transport);
                break;

            case UPLOAD_CHUNK:
                Handlers.handleUploadChunk(packet, session, transport);
                break;

            case UPLOAD_COMPLETE:
                Handlers.handleUploadComplete(packet, session, transport);
                break;

            case DOWNLOAD_INIT:
                Handlers.handleDownloadInit(packet, session, transport);
                break;

            case DOWNLOAD_CHUNK:
                Handlers.handleDownloadChunk(packet, session, transport);
                break;

            case DELETE_FILE:
                Handlers.handleDeleteFile(packet, session, transport);
                break;

            case LOGOUT:
                Handlers.handleLogout(packet, session, transport);
                break;

            default:
                sendError(transport, 400, "Unsupported command", type.toString());
        }
    }

    private static void sendError(PacketTransport transport, int code, String msg, String details) throws Exception {
        DataPacket error = PacketBuilder.buildErrorPacket(code, msg, details);
        transport.send(error);
    }
}
