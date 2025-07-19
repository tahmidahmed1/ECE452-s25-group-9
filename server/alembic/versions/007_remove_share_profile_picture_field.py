"""Remove share_profile_picture field from users table

Revision ID: 007_remove_share_profile_picture_field
Revises: 006_add_in_app_notifications_table
Create Date: 2025-01-19

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '007_remove_share_profile_picture_field'
down_revision = '006_add_in_app_notifications_table'
branch_labels = None
depends_on = None


def upgrade():
    # Remove the share_profile_picture column
    op.drop_column('users', 'share_profile_picture')


def downgrade():
    # Add the share_profile_picture column back
    op.add_column('users', sa.Column('share_profile_picture', sa.Boolean(), nullable=False, server_default='true'))
