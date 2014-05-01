package com.timepath.steam.io.storage.gcf;

import com.timepath.EnumFlag;

/**
 *
 * @author TimePath
 */
enum DirectoryEntryAttributes implements EnumFlag {
    Unknown_4(0x8000), File(0x4000), Unknown_3(0x2000), Unknown_2(0x1000), Executable_File(0x800), Hidden_File(0x400),
    ReadOnly_File(0x200), Encrypted_File(0x100), Purge_File(0x80), Backup_Before_Overwriting(0x40),
    NoCache_File(0x20), Locked_File(0x8), Unknown_1(0x4), Launch_File(0x2), Configuration_File(0x1), Directory(0);

    final int id;

    DirectoryEntryAttributes(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
    
}
