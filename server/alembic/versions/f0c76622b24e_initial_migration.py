"""initial_migration

Revision ID: f0c76622b24e
Revises: 
Create Date: 2025-07-14 21:13:35.484160

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f0c76622b24e'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # Create enums
    usertype_enum = sa.Enum('VOLUNTEER', 'ORGANIZER', name='usertype')
    organizationtype_enum = sa.Enum('NON_PROFIT', 'SCHOOL_GROUP', 'CLUB', 'CHARITY', 'CUSTOM', name='organizationtype')
    sex_enum = sa.Enum('MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY', name='sex')
    
    usertype_enum.create(op.get_bind())
    organizationtype_enum.create(op.get_bind())
    sex_enum.create(op.get_bind())
    
    # Create users table
    op.create_table(
        'users',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('username', sa.String(), nullable=True),
        sa.Column('email', sa.String(), nullable=True),
        sa.Column('hashed_password', sa.String(), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=True),
        sa.Column('user_type', usertype_enum, nullable=True),
        sa.Column('onboarding_completed', sa.Boolean(), nullable=True),
        sa.Column('full_name', sa.String(), nullable=True),
        sa.Column('phone', sa.String(), nullable=True),
        sa.Column('profile_picture_url', sa.String(), nullable=True),
        sa.Column('banner_url', sa.String(), nullable=True),
        sa.Column('organization_name', sa.String(), nullable=True),
        sa.Column('organization_type', organizationtype_enum, nullable=True),
        sa.Column('organization_description', sa.Text(), nullable=True),
        sa.Column('organization_website', sa.String(), nullable=True),
        sa.Column('organization_social_media', sa.JSON(), nullable=True),
        sa.Column('organization_images', sa.JSON(), nullable=True),
        sa.Column('organization_custom_type', sa.String(), nullable=True),
        sa.Column('sex', sex_enum, nullable=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('skills', sa.JSON(), nullable=True),
        sa.Column('age', sa.Integer(), nullable=True),
        sa.Column('emergency_contact_name', sa.String(), nullable=True),
        sa.Column('emergency_contact_phone', sa.String(), nullable=True),
        sa.Column('location_area', sa.String(), nullable=True),
        sa.Column('has_drivers_license', sa.Boolean(), nullable=True),
        sa.Column('disabilities', sa.Text(), nullable=True),
        sa.Column('karma_points', sa.Integer(), nullable=False, default=0),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_users_id'), 'users', ['id'], unique=False)
    op.create_index(op.f('ix_users_username'), 'users', ['username'], unique=True)
    op.create_index(op.f('ix_users_email'), 'users', ['email'], unique=True)
    
    # Create badges table
    op.create_table(
        'badges',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('required_karma_points', sa.Integer(), nullable=False),
        sa.Column('icon_name', sa.String(), nullable=False),
        sa.Column('color', sa.String(), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_badges_id'), 'badges', ['id'], unique=False)
    op.create_index(op.f('ix_badges_name'), 'badges', ['name'], unique=True)
    
    # Create user_badges association table
    op.create_table(
        'user_badges',
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('badge_id', sa.Integer(), nullable=False),
        sa.Column('earned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['badge_id'], ['badges.id'], ),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('user_id', 'badge_id')
    )
    
    # Create messages table
    op.create_table(
        'messages',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('sender_id', sa.Integer(), nullable=False),
        sa.Column('receiver_id', sa.Integer(), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('sent_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_messages_id'), 'messages', ['id'], unique=False)
    
    # Create events table
    op.create_table(
        'events',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('organizer_id', sa.Integer(), nullable=False),
        sa.Column('title', sa.String(), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('date', sa.String(), nullable=True),
        sa.Column('location', sa.String(), nullable=True),
        sa.Column('image_url', sa.String(), nullable=True),
        sa.Column('max_volunteers', sa.Integer(), nullable=True),
        sa.Column('current_volunteers', sa.Integer(), nullable=False, default=0),
        sa.Column('latitude', sa.Float(), nullable=True),
        sa.Column('longitude', sa.Float(), nullable=True),
        sa.Column('karma_points', sa.Integer(), nullable=False, default=10),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_events_id'), 'events', ['id'], unique=False)


def downgrade() -> None:
    """Downgrade schema."""
    # Drop tables in reverse order
    op.drop_index(op.f('ix_events_id'), table_name='events')
    op.drop_table('events')
    
    op.drop_index(op.f('ix_messages_id'), table_name='messages')
    op.drop_table('messages')
    
    op.drop_table('user_badges')
    
    op.drop_index(op.f('ix_badges_name'), table_name='badges')
    op.drop_index(op.f('ix_badges_id'), table_name='badges')
    op.drop_table('badges')
    
    op.drop_index(op.f('ix_users_email'), table_name='users')
    op.drop_index(op.f('ix_users_username'), table_name='users')
    op.drop_index(op.f('ix_users_id'), table_name='users')
    op.drop_table('users')
    
    # Drop enums
    sa.Enum(name='sex').drop(op.get_bind())
    sa.Enum(name='organizationtype').drop(op.get_bind())
    sa.Enum(name='usertype').drop(op.get_bind())
