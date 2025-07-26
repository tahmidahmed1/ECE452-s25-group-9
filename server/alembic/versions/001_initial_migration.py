"""Initial migration

Revision ID: 001_initial_migration
Revises: 
Create Date: 2025-01-15 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '001_initial_migration'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create users table
    op.create_table('users',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('username', sa.String(), nullable=True),
    sa.Column('email', sa.String(), nullable=True),
    sa.Column('hashed_password', sa.String(), nullable=True),
    sa.Column('is_active', sa.Boolean(), nullable=True),
    sa.Column('user_type', sa.Enum('VOLUNTEER', 'ORGANIZER', name='usertype'), nullable=True),
    sa.Column('onboarding_completed', sa.Boolean(), nullable=True),
    sa.Column('full_name', sa.String(), nullable=True),
    sa.Column('phone', sa.String(), nullable=True),
    sa.Column('profile_picture_url', sa.String(), nullable=True),
    sa.Column('banner_url', sa.String(), nullable=True),
    sa.Column('organization_name', sa.String(), nullable=True),
    sa.Column('organization_description', sa.Text(), nullable=True),
    sa.Column('organization_website', sa.String(), nullable=True),
    sa.Column('organization_social_media', sa.JSON(), nullable=True),
    sa.Column('organization_images', sa.JSON(), nullable=True),
    sa.Column('sex', sa.Enum('MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY', name='sex'), nullable=True),
    sa.Column('description', sa.Text(), nullable=True),
    sa.Column('skills', sa.JSON(), nullable=True),
    sa.Column('age', sa.Integer(), nullable=True),
    sa.Column('emergency_contact_name', sa.String(), nullable=True),
    sa.Column('emergency_contact_phone', sa.String(), nullable=True),
    sa.Column('location_area', sa.String(), nullable=True),
    sa.Column('latitude', sa.Float(), nullable=True),
    sa.Column('longitude', sa.Float(), nullable=True),
    sa.Column('karma_points', sa.Integer(), nullable=True),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_users_email'), 'users', ['email'], unique=True)
    op.create_index(op.f('ix_users_id'), 'users', ['id'], unique=False)
    op.create_index(op.f('ix_users_username'), 'users', ['username'], unique=True)
    
    # Create events table
    op.create_table('events',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('organizer_id', sa.Integer(), nullable=False),
    sa.Column('title', sa.String(), nullable=False),
    sa.Column('description', sa.Text(), nullable=False),
    sa.Column('location', sa.String(), nullable=False),
    sa.Column('date_time', sa.DateTime(timezone=True), nullable=False),
    sa.Column('duration_hours', sa.Float(), nullable=True),
    sa.Column('max_volunteers', sa.Integer(), nullable=True),
    sa.Column('karma_points_reward', sa.Integer(), nullable=True),
    sa.Column('requirements', sa.JSON(), nullable=True),
    sa.Column('tags', sa.JSON(), nullable=True),
    sa.Column('status', sa.String(), nullable=True),
    sa.Column('is_active', sa.Boolean(), nullable=True),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['organizer_id'], ['users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_events_id'), 'events', ['id'], unique=False)


def downgrade() -> None:
    # Drop events table
    op.drop_index(op.f('ix_events_id'), table_name='events')
    op.drop_table('events')
    
    # Drop users table
    op.drop_index(op.f('ix_users_username'), table_name='users')
    op.drop_index(op.f('ix_users_id'), table_name='users')
    op.drop_index(op.f('ix_users_email'), table_name='users')
    op.drop_table('users')
    
    # Drop enums
    op.execute("DROP TYPE IF EXISTS usertype")
    op.execute("DROP TYPE IF EXISTS sex")
