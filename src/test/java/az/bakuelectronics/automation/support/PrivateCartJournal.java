package az.bakuelectronics.automation.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;

/** Private durable ownership record. No credentials, cookies, tokens, names or prices. */
public final class PrivateCartJournal {
    private final Path path;

    public PrivateCartJournal(Path path) throws IOException {
        this.path = path;
        if (Files.exists(path))
            throw new IllegalStateException("A previous ownership journal exists. Review recovery before any new mutations.");
        var store = Files.getFileStore(path.toAbsolutePath().getParent());
        if (store.supportsFileAttributeView(PosixFileAttributeView.class)) {
            var permissions = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.createFile(path, PosixFilePermissions.asFileAttribute(permissions));
            if (!Files.getPosixFilePermissions(path).equals(permissions))
                throw new IOException("Private journal POSIX permissions could not be verified");
            return;
        }
        if (!store.supportsFileAttributeView(AclFileAttributeView.class))
            throw new IOException("File system cannot guarantee private journal permissions");
        // Qualify the Windows principal: an unqualified machine/user name can resolve to the domain SID.
        String principal = System.getenv("USERDOMAIN") + "\\" + System.getProperty("user.name");
        var user = path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(principal);
        var acl = List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(user)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build());
        FileAttribute<List<AclEntry>> attribute = new FileAttribute<>() {
            public String name() { return "acl:acl"; }
            public List<AclEntry> value() { return acl; }
        };
        Files.createFile(path, attribute);
        var view = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (view == null || !view.getAcl().equals(acl))
            throw new IOException("Private journal ACL could not be verified");
    }

    public void save(AccountCartOwnership ownership, String stage) throws IOException {
        StringBuilder record = new StringBuilder("phase4-account-cart-v1\nstage=").append(stage).append('\n');
        ownership.baseline().forEach((slug, qty) -> record.append("baseline.").append(slug).append('=').append(qty).append('\n'));
        ownership.owned().forEach((slug, qty) -> record.append("owned.").append(slug).append('=').append(qty).append('\n'));
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(record.toString());
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    public void complete() throws IOException { Files.delete(path); }
}
